package org.nahap.digital_library_backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nahap.digital_library_backend.dto.request.BookmarkCreateRequest;
import org.nahap.digital_library_backend.dto.request.BookmarkUpdateRequest;
import org.nahap.digital_library_backend.dto.response.BookmarkResponse;
import org.nahap.digital_library_backend.security.CustomUserDetails;
import org.nahap.digital_library_backend.service.BookmarkService;
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

    //  USER: POST /api/bookmarks — создать закладку
    @PostMapping
    public ResponseEntity<BookmarkResponse> createBookmark(@Valid @RequestBody BookmarkCreateRequest request,
                                                           @AuthenticationPrincipal CustomUserDetails currentUser) {
        Integer currentUserId = currentUser.getId();
        log.info("Создание закладки пользователем ID: {}", currentUserId);
        BookmarkResponse response = bookmarkService.createBookmark(request, currentUserId);
        return ResponseEntity.status(201).body(response);
    }

    //  USER: PUT /api/bookmarks/{id} — обновить закладку
    @PutMapping("/{id}")
    public ResponseEntity<BookmarkResponse> updateBookmark(@PathVariable Integer id,
                                                           @RequestBody BookmarkUpdateRequest request,
                                                           @AuthenticationPrincipal CustomUserDetails currentUser) {
        Integer currentUserId = currentUser.getId();
        log.info("Обновление закладки ID: {} пользователем {}", id, currentUserId);
        BookmarkResponse response = bookmarkService.updateBookmark(id, request, currentUserId);
        return ResponseEntity.ok(response);
    }

    //  USER: DELETE /api/bookmarks/{id} — удалить закладку
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBookmark(@PathVariable Integer id,
                                               @AuthenticationPrincipal CustomUserDetails currentUser) {
        Integer currentUserId = currentUser.getId();
        log.info("Удаление закладки ID: {} пользователем {}", id, currentUserId);
        bookmarkService.deleteBookmark(id, currentUserId);
        return ResponseEntity.noContent().build();
    }

    //  USER: GET /api/bookmarks/book/{bookId} — мои закладки для книги
    @GetMapping("/book/{bookId}")
    public ResponseEntity<Page<BookmarkResponse>> getBookmarksForBook(
            @PathVariable Integer bookId,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            Pageable pageable) {
        Integer currentUserId = currentUser.getId();
        log.info("Запрос закладок для книги {} от пользователя {}", bookId, currentUserId);

        Page<BookmarkResponse> bookmarks = bookmarkService.getBookmarksByUserAndBook(currentUserId, bookId, pageable);
        return ResponseEntity.ok(bookmarks);
    }
}