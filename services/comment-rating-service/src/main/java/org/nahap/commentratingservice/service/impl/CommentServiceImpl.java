package org.nahap.commentratingservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nahap.commentratingservice.dto.mapper.CommentMapper;
import org.nahap.commentratingservice.dto.request.CommentCreateRequest;
import org.nahap.commentratingservice.dto.response.CommentResponse;
import org.nahap.commentratingservice.entity.Comment;
import org.nahap.commentratingservice.repository.CommentRepository;
import org.nahap.commentratingservice.service.CommentService;
import org.nahap.common.exception.BadRequestException;
import org.nahap.common.exception.ResourceNotFoundException;
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
    private final CommentMapper commentMapper;

    @Override
    @Transactional
    public CommentResponse createComment(CommentCreateRequest request, Integer currentUserId) {
        // Check if user already has active comment for this book
        if (commentRepository.findActiveByUserAndBook(currentUserId, request.bookId()).isPresent()) {
            throw new BadRequestException("You already have a comment on this book");
        }

        Comment comment = new Comment();
        comment.setUserId(currentUserId);
        comment.setBookId(request.bookId());
        comment.setText(request.text().trim());
        comment.setCreatedAt(LocalDateTime.now());

        Comment saved = commentRepository.save(comment);
        log.info("Created comment ID {} from user {} for book {}", saved.getId(), currentUserId, request.bookId());
        return commentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public CommentResponse updateComment(Integer commentId, String newText, Integer currentUserId) {
        Comment comment = commentRepository.findActiveById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found or already deleted"));

        if (!comment.getUserId().equals(currentUserId)) {
            throw new AccessDeniedException("You cannot edit someone else's comment");
        }

        comment.setText(newText.trim());
        Comment updated = commentRepository.save(comment);
        log.info("Updated comment ID {} by user {}", commentId, currentUserId);
        return commentMapper.toResponse(updated);
    }

    @Override
    public Page<CommentResponse> getActiveCommentsByBook(Integer bookId, Pageable pageable) {
        return commentRepository.findActiveByBookId(bookId, pageable)
                .map(commentMapper::toResponse);
    }

    @Override
    public List<CommentResponse> getAllCommentsByBookForModerator(Integer bookId) {
        return commentRepository.findAllByBookId(bookId).stream()
                .map(commentMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void softDeleteComment(Integer commentId, Integer currentUserId, boolean isAdminOrModerator) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        if (!isAdminOrModerator && !comment.getUserId().equals(currentUserId)) {
            throw new AccessDeniedException("You cannot delete someone else's comment");
        }

        if (comment.getDeletedAt() != null) {
            log.warn("Attempted to delete already deleted comment ID {}", commentId);
            return;
        }

        comment.setDeletedAt(LocalDateTime.now());
        commentRepository.save(comment);
        log.info("Comment ID {} deleted by user {} (admin/moderator: {})", commentId, currentUserId, isAdminOrModerator);
    }

    @Override
    @Transactional
    public void restoreComment(Integer commentId, Integer currentUserId, boolean isAdminOrModerator) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        if (!isAdminOrModerator) {
            throw new AccessDeniedException("Only moderator or administrator can restore comments");
        }

        if (comment.getDeletedAt() == null) {
            log.warn("Attempted to restore non-deleted comment ID {}", commentId);
            return;
        }

        // Check if user already has active comment for this book
        Integer userId = comment.getUserId();
        Integer bookId = comment.getBookId();

        if (commentRepository.findActiveByUserAndBook(userId, bookId).isPresent()) {
            throw new BadRequestException(
                    "Cannot restore comment: user already has an active comment on this book"
            );
        }

        comment.setDeletedAt(null);
        commentRepository.save(comment);
        log.info("Comment ID {} restored by moderator/admin {}", commentId, currentUserId);
    }
}
