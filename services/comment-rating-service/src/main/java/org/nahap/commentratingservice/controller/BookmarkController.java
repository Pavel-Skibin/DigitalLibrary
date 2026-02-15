package org.nahap.commentratingservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nahap.commentratingservice.dto.request.BookmarkCreateRequest;
import org.nahap.commentratingservice.dto.request.BookmarkUpdateRequest;
import org.nahap.commentratingservice.dto.response.BookmarkResponse;
import org.nahap.commentratingservice.security.JwtUserPrincipal;
import org.nahap.commentratingservice.service.BookmarkService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;

    // USER: POST /api/bookmarks - create bookmark
    @PostMapping
    public ResponseEntity<BookmarkResponse> createBookmark(
            @Valid @RequestBody BookmarkCreateRequest request,
            @AuthenticationPrincipal JwtUserPrincipal currentUser) {
        Integer currentUserId = currentUser.getUserId();
        log.info("Creating bookmark by user ID: {}", currentUserId);
        BookmarkResponse response = bookmarkService.createBookmark(request, currentUserId);
        return ResponseEntity.status(201).body(response);
    }

    // USER: PUT /api/bookmarks/{id} - update bookmark
    @PutMapping("/{id}")
    public ResponseEntity<BookmarkResponse> updateBookmark(
            @PathVariable Integer id,
            @RequestBody BookmarkUpdateRequest request,
            @AuthenticationPrincipal JwtUserPrincipal currentUser) {
        Integer currentUserId = currentUser.getUserId();
        log.info("Updating bookmark ID: {} by user {}", id, currentUserId);
        BookmarkResponse response = bookmarkService.updateBookmark(id, request, currentUserId);
        return ResponseEntity.ok(response);
    }

    // USER: DELETE /api/bookmarks/{id} - delete bookmark
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBookmark(
            @PathVariable Integer id,
            @AuthenticationPrincipal JwtUserPrincipal currentUser) {
        Integer currentUserId = currentUser.getUserId();
        log.info("Deleting bookmark ID: {} by user {}", id, currentUserId);
        bookmarkService.deleteBookmark(id, currentUserId);
        return ResponseEntity.noContent().build();
    }

    // USER: GET /api/bookmarks/book/{bookId} - my bookmarks for a book
    @GetMapping("/book/{bookId}")
    public ResponseEntity<Page<BookmarkResponse>> getBookmarksForBook(
            @PathVariable Integer bookId,
            @AuthenticationPrincipal JwtUserPrincipal currentUser,
            Pageable pageable) {
        Integer currentUserId = currentUser.getUserId();
        log.info("Request for bookmarks for book {} from user {}", bookId, currentUserId);
        Page<BookmarkResponse> bookmarks = bookmarkService.getBookmarksByUserAndBook(currentUserId, bookId, pageable);
        return ResponseEntity.ok(bookmarks);
    }
}
