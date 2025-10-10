package org.nahap.digital_library_backend.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nahap.digital_library_backend.dto.request.CommentCreateRequest;
import org.nahap.digital_library_backend.dto.response.CommentResponse;
import org.nahap.digital_library_backend.security.CustomUserDetails;
import org.nahap.digital_library_backend.service.CommentService;
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

    //  Гость: GET /api/comments/books/{bookId} — активные комментарии к книге
    @GetMapping("/books/{bookId}")
    public ResponseEntity<Page<CommentResponse>> getCommentsByBook(
            @PathVariable Integer bookId,
            Pageable pageable) {
        log.info("Запрос активных комментариев к книге ID: {}", bookId);
        Page<CommentResponse> comments = commentService.getActiveCommentsByBook(bookId, pageable);
        return ResponseEntity.ok(comments);
    }

    //  MODERATOR/ADMIN: GET /api/comments/books/{bookId}/all — все комментарии (включая удалённые)
    @GetMapping("/books/{bookId}/all")
    public ResponseEntity<List<CommentResponse>> getAllCommentsByBookForModerator(
            @PathVariable Integer bookId) {
        log.info("Запрос ВСЕХ комментариев к книге ID: {} (модерация)", bookId);
        List<CommentResponse> comments = commentService.getAllCommentsByBookForModerator(bookId);
        return ResponseEntity.ok(comments);
    }

    //  USER: POST /api/comments — оставить комментарий
    @PostMapping
    public ResponseEntity<CommentResponse> createComment(
            @RequestBody CommentCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        Integer currentUserId = currentUser.getId();
        log.info("Создание комментария от пользователя ID: {}", currentUserId);
        CommentResponse response = commentService.createComment(request, currentUserId);
        return ResponseEntity.status(201).body(response);
    }

    //  USER: PUT /api/comments/{id} — редактировать свой комментарий
    @PutMapping("/{id}")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable Integer id,
            @RequestBody String newText,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        Integer currentUserId = currentUser.getId();
        log.info("Обновление комментария ID: {} пользователем {}", id, currentUserId);
        CommentResponse response = commentService.updateComment(id, newText, currentUserId);
        return ResponseEntity.ok(response);
    }

    //  USER: DELETE /api/comments/{id} — удалить свой комментарий
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOwnComment(
            @PathVariable Integer id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        Integer currentUserId = currentUser.getId();
        log.info("Пользователь {} удаляет свой комментарий ID: {}", currentUserId, id);
        commentService.softDeleteComment(id, currentUserId, false);
        return ResponseEntity.noContent().build();
    }

    //  MODERATOR/ADMIN: DELETE /api/comments/{id}/moderate — удалить любой комментарий
    @DeleteMapping("/{id}/moderate")
    public ResponseEntity<Void> moderateDeleteComment(
            @PathVariable Integer id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        Integer currentUserId = currentUser.getId();
        log.info("Модератор/админ {} удаляет комментарий ID: {}", currentUserId, id);
        commentService.softDeleteComment(id, currentUserId, true);
        return ResponseEntity.noContent().build();
    }


    @PostMapping("/{id}/restore")
    public ResponseEntity<Void> restoreComment(
            @PathVariable Integer id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        Integer currentUserId = currentUser.getId();
        log.info("Модератор/админ {} восстанавливает комментарий ID: {}", currentUserId, id);
        commentService.restoreComment(id, currentUserId, true);
        return ResponseEntity.ok().build();
    }

}