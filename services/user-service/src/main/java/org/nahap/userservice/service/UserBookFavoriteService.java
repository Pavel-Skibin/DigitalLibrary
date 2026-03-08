package org.nahap.userservice.service;

import lombok.extern.slf4j.Slf4j;
import org.nahap.userservice.dto.response.FavoriteResponse;
import org.nahap.userservice.dto.response.FavoriteStatusResponse;
import org.nahap.userservice.entity.UserBookFavorite;
import org.nahap.userservice.repository.UserBookFavoriteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Сервис для работы с избранными книгами
 */
@Slf4j
@Service
public class UserBookFavoriteService {

    private final UserBookFavoriteRepository favoriteRepository;

    public UserBookFavoriteService(UserBookFavoriteRepository favoriteRepository) {
        this.favoriteRepository = favoriteRepository;
    }

    /**
     * Добавить книгу в избранное
     */
    @Transactional
    public FavoriteResponse addToFavorites(Integer userId, Integer bookId) {
        // Проверить, не в избранном ли уже
        if (favoriteRepository.existsByUserIdAndBookId(userId, bookId)) {
            log.warn("Book already in favorites: user={}, book={}", userId, bookId);
            throw new IllegalArgumentException("Book already in favorites");
        }

        UserBookFavorite favorite = UserBookFavorite.builder()
                .userId(userId)
                .bookId(bookId)
                .build();

        favorite = favoriteRepository.save(favorite);
        log.info("Added to favorites: user={}, book={}", userId, bookId);

        return mapToResponse(favorite);
    }

    /**
     * Удалить из избранного
     */
    @Transactional
    public void removeFromFavorites(Integer userId, Integer bookId) {
        UserBookFavorite favorite = favoriteRepository.findByUserIdAndBookId(userId, bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not in favorites"));

        favoriteRepository.delete(favorite);
        log.info("Removed from favorites: user={}, book={}", userId, bookId);
    }

    /**
     * Получить избранные книги пользователя
     */
    @Transactional(readOnly = true)
    public Page<FavoriteResponse> getUserFavorites(Integer userId, Pageable pageable) {
        Page<UserBookFavorite> favorites = favoriteRepository.findByUserIdOrderByAddedAtDesc(userId, pageable);
        return favorites.map(this::mapToResponse);
    }

    /**
     * Получить список ID избранных книг
     */
    @Transactional(readOnly = true)
    public List<Integer> getUserFavoriteBookIds(Integer userId) {
        return favoriteRepository.findBookIdsByUserId(userId);
    }

    /**
     * Проверить, в избранном ли книга
     */
    @Transactional(readOnly = true)
    public FavoriteStatusResponse checkFavoriteStatus(Integer userId, Integer bookId) {
        boolean isFavorite = favoriteRepository.existsByUserIdAndBookId(userId, bookId);
        return FavoriteStatusResponse.builder()
                .isFavorite(isFavorite)
                .build();
    }

    /**
     * Количество избранных книг у пользователя
     */
    @Transactional(readOnly = true)
    public Long countUserFavorites(Integer userId) {
        return favoriteRepository.countByUserId(userId);
    }

    /**
     * Количество пользователей, добавивших книгу в избранное
     */
    @Transactional(readOnly = true)
    public Long countBookFavorites(Integer bookId) {
        return favoriteRepository.countByBookId(bookId);
    }

    private FavoriteResponse mapToResponse(UserBookFavorite favorite) {
        return FavoriteResponse.builder()
                .id(favorite.getId())
                .bookId(favorite.getBookId())
                .addedAt(favorite.getAddedAt())
                .build();
    }
}
