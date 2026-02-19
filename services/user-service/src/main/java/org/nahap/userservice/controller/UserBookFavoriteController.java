package org.nahap.userservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.nahap.userservice.dto.response.FavoriteResponse;
import org.nahap.userservice.dto.response.FavoriteStatusResponse;
import org.nahap.userservice.security.CustomUserDetails;
import org.nahap.userservice.service.UserBookFavoriteService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Контроллер для работы с избранными книгами
 */
@Slf4j
@RestController
@RequestMapping("/api/favorites")
public class UserBookFavoriteController {

    private final UserBookFavoriteService favoriteService;

    public UserBookFavoriteController(UserBookFavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    /**
     * Получить избранные книги
     * GET /api/favorites
     */
    @GetMapping
    public ResponseEntity<Page<FavoriteResponse>> getMyFavorites(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PageableDefault(size = 50) Pageable pageable
    ) {
        Integer userId = currentUser.getId();
        log.info("Getting favorites for user: {}", userId);
        Page<FavoriteResponse> favorites = favoriteService.getUserFavorites(userId, pageable);
        return ResponseEntity.ok(favorites);
    }

    /**
     * Получить ID избранных книг
     * GET /api/favorites/book-ids
     */
    @GetMapping("/book-ids")
    public ResponseEntity<List<Integer>> getMyFavoriteBookIds(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        Integer userId = currentUser.getId();
        List<Integer> bookIds = favoriteService.getUserFavoriteBookIds(userId);
        return ResponseEntity.ok(bookIds);
    }

    /**
     * Добавить книгу в избранное
     * POST /api/favorites/{bookId}
     */
    @PostMapping("/{bookId}")
    public ResponseEntity<FavoriteResponse> addToFavorites(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Integer bookId
    ) {
        Integer userId = currentUser.getId();
        log.info("Adding to favorites: user={}, book={}", userId, bookId);
        FavoriteResponse response = favoriteService.addToFavorites(userId, bookId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Удалить из избранного
     * DELETE /api/favorites/{bookId}
     */
    @DeleteMapping("/{bookId}")
    public ResponseEntity<Void> removeFromFavorites(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Integer bookId
    ) {
        Integer userId = currentUser.getId();
        log.info("Removing from favorites: user={}, book={}", userId, bookId);
        favoriteService.removeFromFavorites(userId, bookId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Проверить статус избранного
     * GET /api/favorites/{bookId}/status
     */
    @GetMapping("/{bookId}/status")
    public ResponseEntity<FavoriteStatusResponse> getFavoriteStatus(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Integer bookId
    ) {
        Integer userId = currentUser.getId();
        FavoriteStatusResponse status = favoriteService.checkFavoriteStatus(userId, bookId);
        return ResponseEntity.ok(status);
    }
}
