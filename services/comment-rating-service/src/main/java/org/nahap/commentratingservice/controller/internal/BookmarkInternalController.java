package org.nahap.commentratingservice.controller.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nahap.commentratingservice.api.internal.InternalBookmarkApiApi;
import org.nahap.commentratingservice.repository.BookmarkRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal API Controller for Bookmark statistics
 * Implements OpenAPI generated interface
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class BookmarkInternalController implements InternalBookmarkApiApi {

    private final BookmarkRepository bookmarkRepository;

    @Override
    public ResponseEntity<Long> getBookmarksCount(Integer bookId) {
        log.debug("Internal API: Getting bookmarks count for book: {}", bookId);
        
        Long count = bookmarkRepository.countActiveBookmarksByBookId(bookId);
        
        return ResponseEntity.ok(count);
    }
}
