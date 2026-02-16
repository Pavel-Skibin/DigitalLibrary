package org.nahap.commentratingservice.controller.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nahap.commentratingservice.api.internal.InternalCommentApiApi;
import org.nahap.commentratingservice.repository.CommentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal API Controller for Comment statistics
 * Implements OpenAPI generated interface
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class CommentInternalController implements InternalCommentApiApi {

    private final CommentRepository commentRepository;

    @Override
    public ResponseEntity<Long> getCommentsCount(Integer bookId) {
        log.debug("Internal API: Getting comments count for book: {}", bookId);
        
        Long count = commentRepository.countActiveCommentsByBookId(bookId);
        
        return ResponseEntity.ok(count);
    }
}
