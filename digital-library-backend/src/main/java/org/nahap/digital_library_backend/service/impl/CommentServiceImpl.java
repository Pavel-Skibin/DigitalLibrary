package org.nahap.digital_library_backend.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nahap.digital_library_backend.dto.mapper.CommentMapper;
import org.nahap.digital_library_backend.dto.request.CommentCreateRequest;
import org.nahap.digital_library_backend.dto.response.CommentResponse;
import org.nahap.digital_library_backend.entity.Book;
import org.nahap.digital_library_backend.entity.Comment;
import org.nahap.digital_library_backend.entity.User;
import org.nahap.digital_library_backend.exception.*;
import org.nahap.digital_library_backend.repository.BookRepository;
import org.nahap.digital_library_backend.repository.CommentRepository;
import org.nahap.digital_library_backend.repository.UserRepository;
import org.nahap.digital_library_backend.service.CommentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final CommentMapper commentMapper;

    @Override
    @Transactional
    public CommentResponse createComment(CommentCreateRequest request, Integer currentUserId) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));
        Book book = bookRepository.findById(request.bookId())
                .orElseThrow(() -> new BookNotFoundException("Книга не найдена"));

        if (commentRepository.findActiveByUserAndBook(currentUserId, request.bookId()).isPresent()) {
            throw new CommentAlreadyExistsException("Вы уже оставили комментарий к этой книге");
        }

        Comment comment = new Comment();
        comment.setUser(user);
        comment.setBook(book);
        comment.setText(request.text().trim());
        comment.setCreatedAt(LocalDateTime.now());

        Comment saved = commentRepository.save(comment);
        log.info("Создан комментарий ID {} от пользователя {} к книге {}", saved.getId(), currentUserId, request.bookId());
        return commentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public CommentResponse updateComment(Integer commentId, String newText, Integer currentUserId) {
        Comment comment = commentRepository.findActiveById(commentId)
                .orElseThrow(() -> new CommentNotFoundException("Комментарий не найден или уже удалён"));

        if (!comment.getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("Вы не можете редактировать чужой комментарий");
        }

        comment.setText(newText.trim());
        Comment updated = commentRepository.save(comment);
        log.info("Обновлён комментарий ID {} пользователем {}", commentId, currentUserId);
        return commentMapper.toResponse(updated);
    }


    @Override
    public Page<CommentResponse> getActiveCommentsByBook(Integer bookId, Pageable pageable) {
        if (!bookRepository.existsById(bookId)) {
            throw new BookNotFoundException("Книга не найдена");
        }
        return commentRepository.findActiveByBookId(bookId, pageable)
                .map(commentMapper::toResponse);
    }

    @Override
    public List<CommentResponse> getAllCommentsByBookForModerator(Integer bookId) {
        if (!bookRepository.existsById(bookId)) {
            throw new BookNotFoundException("Книга не найдена");
        }
        return commentRepository.findAllByBookId(bookId).stream()
                .map(commentMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void softDeleteComment(Integer commentId, Integer currentUserId, boolean isAdminOrModerator) {
        Comment comment = commentRepository.findByIdWithRelations(commentId)  // <-- ИЗМЕНЕНО
                .orElseThrow(() -> new CommentNotFoundException("Комментарий не найден"));

        if (!isAdminOrModerator && !comment.getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("Вы не можете удалить чужой комментарий");
        }

        if (comment.getDeletedAt() != null) {
            log.warn("Попытка повторного удаления комментария ID {}", commentId);
            return;
        }

        comment.setDeletedAt(LocalDateTime.now());
        commentRepository.save(comment);
        log.info("Комментарий ID {} удалён пользователем {} (админ/модератор: {})", commentId, currentUserId, isAdminOrModerator);
    }

    @Override
    @Transactional
    public void restoreComment(Integer commentId, Integer currentUserId, boolean isAdminOrModerator) {
        Comment comment = commentRepository.findByIdWithRelations(commentId)
                .orElseThrow(() -> new CommentNotFoundException("Комментарий не найден"));

        if (!isAdminOrModerator) {
            throw new AccessDeniedException("Только модератор или администратор может восстанавливать комментарии");
        }

        if (comment.getDeletedAt() == null) {
            log.warn("Попытка восстановить неудалённый комментарий ID {}", commentId);
            return;
        }

        Integer userId = comment.getUser().getId();
        Integer bookId = comment.getBook().getId();

        if (commentRepository.findActiveByUserAndBook(userId, bookId).isPresent()) {
            throw new CommentAlreadyExistsException(
                    "Невозможно восстановить комментарий: у пользователя уже есть активный комментарий к этой книге"
            );
        }

        comment.setDeletedAt(null);
        commentRepository.save(comment);
        log.info("Комментарий ID {} восстановлен модератором/админом {}", commentId, currentUserId);
    }
}