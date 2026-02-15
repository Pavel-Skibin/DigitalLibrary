package org.nahap.commentratingservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nahap.commentratingservice.dto.request.CommentCreateRequest;
import org.nahap.commentratingservice.dto.response.CommentResponse;
import org.nahap.commentratingservice.security.JwtUserPrincipal;
import org.nahap.commentratingservice.service.CommentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // Guest: GET /api/comments/books/{bookId} - active comments for book
    @GetMapping("/books/{bookId}")
    public ResponseEntity<Page<CommentResponse>> getCommentsByBook(
            @PathVariable Integer bookId,
            Pageable pageable) {
        log.info("Request for active comments for book ID: {}", bookId);
        Page<CommentResponse> comments = commentService.getActiveCommentsByBook(bookId, pageable);
        return ResponseEntity.ok(comments);
    }

    // MODERATOR/ADMIN: GET /api/comments/books/{bookId}/all - all comments (including deleted)
    @GetMapping("/books/{bookId}/all")
    public ResponseEntity<List<CommentResponse>> getAllCommentsByBookForModerator(
            @PathVariable Integer bookId) {
        log.info("Request for ALL comments for book ID: {} (moderation)", bookId);
        List<CommentResponse> comments = commentService.getAllCommentsByBookForModerator(bookId);
        return ResponseEntity.ok(comments);
    }

    // USER: POST /api/comments - create comment
    @PostMapping
    public ResponseEntity<CommentResponse> createComment(
            @RequestBody CommentCreateRequest request,
            @AuthenticationPrincipal JwtUserPrincipal currentUser) {
        Integer currentUserId = currentUser.getUserId();
        log.info("Creating comment from user ID: {}", currentUserId);
        CommentResponse response = commentService.createComment(request, currentUserId);
        return ResponseEntity.status(201).body(response);
    }

    // USER: PUT /api/comments/{id} - edit own comment
    @PutMapping("/{id}")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable Integer id,
            @RequestBody String newText,
            @AuthenticationPrincipal JwtUserPrincipal currentUser) {
        Integer currentUserId = currentUser.getUserId();
        log.info("Updating comment ID: {} by user {}", id, currentUserId);
        CommentResponse response = commentService.updateComment(id, newText, currentUserId);
        return ResponseEntity.ok(response);
    }

    // USER: DELETE /api/comments/{id} - delete own comment
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOwnComment(
            @PathVariable Integer id,
            @AuthenticationPrincipal JwtUserPrincipal currentUser) {
        Integer currentUserId = currentUser.getUserId();
        log.info("User {} deleting own comment ID: {}", currentUserId, id);
        commentService.softDeleteComment(id, currentUserId, false);
        return ResponseEntity.noContent().build();
    }

    // MODERATOR/ADMIN: DELETE /api/comments/{id}/moderate - delete any comment
    @DeleteMapping("/{id}/moderate")
    public ResponseEntity<Void> moderateDeleteComment(
            @PathVariable Integer id,
            @AuthenticationPrincipal JwtUserPrincipal currentUser) {
        Integer currentUserId = currentUser.getUserId();
        boolean isModeratorOrAdmin = currentUser.hasRole("ROLE_MODERATOR") || currentUser.hasRole("ROLE_ADMIN");
        log.info("Moderator/Admin {} deleting comment ID: {}", currentUserId, id);
        commentService.softDeleteComment(id, currentUserId, isModeratorOrAdmin);
        return ResponseEntity.noContent().build();
    }

    // MODERATOR/ADMIN: POST /api/comments/{id}/restore - restore deleted comment
    @PostMapping("/{id}/restore")
    public ResponseEntity<Void> restoreComment(
            @PathVariable Integer id,
            @AuthenticationPrincipal JwtUserPrincipal currentUser) {
        Integer currentUserId = currentUser.getUserId();
        boolean isModeratorOrAdmin = currentUser.hasRole("ROLE_MODERATOR") || currentUser.hasRole("ROLE_ADMIN");
        log.info("Moderator/Admin {} restoring comment ID: {}", currentUserId, id);
        commentService.restoreComment(id, currentUserId, isModeratorOrAdmin);
        return ResponseEntity.ok().build();
    }
}
