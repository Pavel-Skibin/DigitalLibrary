package org.nahap.commentratingservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nahap.commentratingservice.client.book.InternalBookApiApi;
import org.nahap.commentratingservice.client.user.InternalUserApiApi;
import org.nahap.commentratingservice.dto.mapper.BookmarkMapper;
import org.nahap.commentratingservice.dto.request.BookmarkCreateRequest;
import org.nahap.commentratingservice.dto.request.BookmarkUpdateRequest;
import org.nahap.commentratingservice.dto.response.BookmarkResponse;
import org.nahap.commentratingservice.entity.Bookmark;
import org.nahap.commentratingservice.repository.BookmarkRepository;
import org.nahap.commentratingservice.service.BookmarkService;
import org.nahap.common.exception.ResourceNotFoundException;
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
    private final BookmarkMapper bookmarkMapper;
    private final InternalUserApiApi userServiceClient;
    private final InternalBookApiApi bookServiceClient;

    @Override
    @Transactional
    public BookmarkResponse createBookmark(BookmarkCreateRequest request, Integer currentUserId) {
        // Validate user exists
        var userValidation = userServiceClient.validateUser(currentUserId);
        if (!userValidation.getExists()) {
            throw new ResourceNotFoundException("User not found: " + currentUserId);
        }

        // Validate book exists
        var bookValidation = bookServiceClient.validateBook(request.bookId());
        if (!bookValidation.getExists()) {
            throw new ResourceNotFoundException("Book not found: " + request.bookId());
        }

        Bookmark bookmark = new Bookmark();
        bookmark.setUserId(currentUserId);
        bookmark.setBookId(request.bookId());
        bookmark.setPosition(request.position());
        bookmark.setName(request.name());
        bookmark.setNotes(request.notes());
        bookmark.setCreatedAt(LocalDateTime.now());

        Bookmark saved = bookmarkRepository.save(bookmark);
        log.info("Created bookmark ID {} by user {} for book {}", saved.getId(), currentUserId, request.bookId());
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
        log.info("Updated bookmark ID {} by user {}", bookmarkId, currentUserId);
        return bookmarkMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteBookmark(Integer bookmarkId, Integer currentUserId) {
        Bookmark bookmark = findActiveBookmarkOrThrow(bookmarkId);

        bookmark.setDeletedAt(LocalDateTime.now());
        bookmarkRepository.save(bookmark);
        log.info("Deleted bookmark ID {} by user {}", bookmarkId, currentUserId);
    }

    @Override
    public List<BookmarkResponse> getBookmarksByUser(Integer userId, Pageable pageable) {
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
        Page<Bookmark> bookmarks = bookmarkRepository.findActiveByUserIdAndBookId(userId, bookId, pageable);
        return bookmarks.map(bookmarkMapper::toResponse);
    }

    private Bookmark findActiveBookmarkOrThrow(Integer bookmarkId) {
        return bookmarkRepository.findActiveById(bookmarkId)
                .orElseThrow(() -> new ResourceNotFoundException("Bookmark not found or deleted"));
    }
}
