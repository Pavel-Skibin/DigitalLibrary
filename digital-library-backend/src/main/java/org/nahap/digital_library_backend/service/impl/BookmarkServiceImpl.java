package org.nahap.digital_library_backend.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nahap.digital_library_backend.dto.mapper.BookmarkMapper;
import org.nahap.digital_library_backend.dto.request.BookmarkCreateRequest;
import org.nahap.digital_library_backend.dto.request.BookmarkUpdateRequest;
import org.nahap.digital_library_backend.dto.response.BookmarkResponse;
import org.nahap.digital_library_backend.entity.Book;
import org.nahap.digital_library_backend.entity.Bookmark;
import org.nahap.digital_library_backend.entity.User;
import org.nahap.digital_library_backend.exception.*;
import org.nahap.digital_library_backend.repository.BookRepository;
import org.nahap.digital_library_backend.repository.BookmarkRepository;
import org.nahap.digital_library_backend.repository.UserRepository;
import org.nahap.digital_library_backend.service.BookmarkService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookmarkServiceImpl implements BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final UserRepository userRepository;

    private final BookRepository bookRepository;
    private final BookmarkMapper bookmarkMapper;

    @Override
    @Transactional
    public BookmarkResponse createBookmark(BookmarkCreateRequest request, Integer currentUserId) {
        User user = findUserOrThrow(currentUserId);
        Book book = findBookOrThrow(request.bookId());

        Bookmark bookmark = new Bookmark();
        bookmark.setUser(user);
        bookmark.setBook(book);
        bookmark.setPosition(request.position());
        bookmark.setName(request.name());
        bookmark.setNotes(request.notes());
        bookmark.setCreatedAt(LocalDateTime.now());

        Bookmark saved = bookmarkRepository.save(bookmark);
        log.info("Создана закладка ID {} пользователем {} для книги {}", saved.getId(), currentUserId, request.bookId());
        return bookmarkMapper.toResponse(saved);
    }


    @Override
    @Transactional
    public BookmarkResponse updateBookmark(Integer bookmarkId, BookmarkUpdateRequest request, Integer currentUserId) {
        Bookmark bookmark = findActiveBookmarkOrThrow(bookmarkId);

        bookmark.setPosition(request.position());
        bookmark.setName(request.name());
        bookmark.setNotes(request.notes());

        Bookmark updated = bookmarkRepository.save(bookmark);
        log.info("Обновлена закладка ID {} пользователем {}", bookmarkId, currentUserId);
        return bookmarkMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteBookmark(Integer bookmarkId, Integer currentUserId) {
        Bookmark bookmark = findActiveBookmarkOrThrow(bookmarkId);

        bookmark.setDeletedAt(LocalDateTime.now());
        bookmarkRepository.save(bookmark);
        log.info("Закладка ID {} удалена пользователем {}", bookmarkId, currentUserId);
    }

    @Override
    public List<BookmarkResponse> getBookmarksByUser(Integer userId, Pageable pageable) {
        findUserOrThrow(userId);
        return bookmarkRepository.findActiveByUserId(userId, pageable).stream()
                .map(bookmarkMapper::toResponse)
                .toList();
    }

    @Override
    public BookmarkResponse getBookmarkById(Integer bookmarkId, Integer currentUserId) {
        Bookmark bookmark = findActiveBookmarkOrThrow(bookmarkId);

        return bookmarkMapper.toResponse(bookmark);
    }

    @Override
    public Page<BookmarkResponse> getBookmarksByUserAndBook(Integer userId, Integer bookId, Pageable pageable) {
        findUserOrThrow(userId);
        findBookOrThrow(bookId);

        Page<Bookmark> bookmarks = bookmarkRepository.findActiveByUserIdAndBookId(userId, bookId, pageable);
        return bookmarks.map(bookmarkMapper::toResponse);
    }

    // === ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ===

    private User findUserOrThrow(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));
    }

    private Book findBookOrThrow(Integer bookId) {
        return bookRepository.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException("Книга не найдена"));
    }

    private Bookmark findActiveBookmarkOrThrow(Integer bookmarkId) {
        return bookmarkRepository.findActiveById(bookmarkId)
                .orElseThrow(() -> new BookmarkNotFoundException("Закладка не найдена или удалена"));
    }
}