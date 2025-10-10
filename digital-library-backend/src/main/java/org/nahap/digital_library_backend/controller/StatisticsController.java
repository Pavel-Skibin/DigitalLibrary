// src/main/java/org/nahap/digital_library_backend/controller/StatisticsController.java
package org.nahap.digital_library_backend.controller;

import lombok.RequiredArgsConstructor;
import org.nahap.digital_library_backend.dto.response.*;
import org.nahap.digital_library_backend.service.StatisticsService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    // === ОБЩАЯ СТАТИСТИКА СИСТЕМЫ ===

    @GetMapping("/system")
    public ResponseEntity<SystemStatisticsDTO> getSystemStatistics() {
        return ResponseEntity.ok(statisticsService.getSystemStatistics());
    }

    // === СТАТИСТИКА ПО КНИГАМ ===

    @GetMapping("/books/top-rated")
    public ResponseEntity<Page<BookStatDTO>> getTopRatedBooks(
            @RequestParam(defaultValue = "3") Long minRatings,
            Pageable pageable) {
        return ResponseEntity.ok(statisticsService.getTopRatedBooks(minRatings, pageable));
    }

    @GetMapping("/books/most-commented")
    public ResponseEntity<Page<BookStatDTO>> getMostCommentedBooks(Pageable pageable) {
        return ResponseEntity.ok(statisticsService.getMostCommentedBooks(pageable));
    }

    @GetMapping("/books/most-bookmarked")
    public ResponseEntity<Page<BookStatDTO>> getMostBookmarkedBooks(Pageable pageable) {
        return ResponseEntity.ok(statisticsService.getMostBookmarkedBooks(pageable));
    }

    @GetMapping("/books/without-ratings")
    public ResponseEntity<List<BookResponse>> getBooksWithoutRatings() {
        return ResponseEntity.ok(statisticsService.getBooksWithoutRatings());
    }

    @GetMapping("/books/total-count")
    public ResponseEntity<Long> getTotalBooksCount() {
        return ResponseEntity.ok(statisticsService.getTotalBooksCount());
    }

    @GetMapping("/books/count-by-genre/{genreId}")
    public ResponseEntity<Long> getBookCountByGenre(@PathVariable Integer genreId) {
        return ResponseEntity.ok(statisticsService.getBookCountByGenre(genreId));
    }

    @GetMapping("/books/count-by-author/{authorId}")
    public ResponseEntity<Long> getBookCountByAuthor(@PathVariable Integer authorId) {
        return ResponseEntity.ok(statisticsService.getBookCountByAuthor(authorId));
    }

    // === СТАТИСТИКА ПО ЖАНРАМ ===

    @GetMapping("/genres/top-by-count")
    public ResponseEntity<Page<GenreStatDTO>> getTopGenresByBookCount(Pageable pageable) {
        return ResponseEntity.ok(statisticsService.getTopGenresByBookCount(pageable));
    }

    @GetMapping("/genres/top-by-rating")
    public ResponseEntity<Page<GenreStatDTO>> getTopGenresByRating(
            @RequestParam(defaultValue = "3") Long minRatings,
            Pageable pageable) {
        return ResponseEntity.ok(statisticsService.getTopGenresByRating(minRatings, pageable));
    }

    // === СТАТИСТИКА ПО АВТОРАМ ===

    @GetMapping("/authors/top-by-count")
    public ResponseEntity<Page<AuthorStatDTO>> getTopAuthorsByBookCount(Pageable pageable) {
        return ResponseEntity.ok(statisticsService.getTopAuthorsByBookCount(pageable));
    }

    @GetMapping("/authors/top-by-rating")
    public ResponseEntity<Page<AuthorStatDTO>> getTopAuthorsByRating(
            @RequestParam(defaultValue = "3") Long minRatings,
            Pageable pageable) {
        return ResponseEntity.ok(statisticsService.getTopAuthorsByRating(minRatings, pageable));
    }

    // === СТАТИСТИКА ПО ПОЛЬЗОВАТЕЛЯМ ===

    @GetMapping("/users/active-count")
    public ResponseEntity<Long> getActiveUsersCount() {
        return ResponseEntity.ok(statisticsService.getActiveUsersCount());
    }

    @GetMapping("/users/by-role")
    public ResponseEntity<List<UserRoleCountDTO>> getUserCountByRole() {
        return ResponseEntity.ok(statisticsService.getUserCountByRole());
    }

    @GetMapping("/users/most-active-commenters")
    public ResponseEntity<Page<UserActivityDTO>> getMostActiveCommenters(Pageable pageable) {
        return ResponseEntity.ok(statisticsService.getMostActiveCommenters(pageable));
    }

    @GetMapping("/users/most-active-raters")
    public ResponseEntity<Page<UserActivityDTO>> getMostActiveRaters(Pageable pageable) {
        return ResponseEntity.ok(statisticsService.getMostActiveRaters(pageable));
    }

    // === СТАТИСТИКА ПО КОММЕНТАРИЯМ ===

    @GetMapping("/comments/active-count")
    public ResponseEntity<Long> getActiveCommentsCount() {
        return ResponseEntity.ok(statisticsService.getActiveCommentsCount());
    }

    @GetMapping("/comments/total-count")
    public ResponseEntity<Long> getTotalCommentsCount() {
        return ResponseEntity.ok(statisticsService.getTotalCommentsCount());
    }

    @GetMapping("/comments/deleted-percentage")
    public ResponseEntity<Double> getDeletedCommentsPercentage() {
        return ResponseEntity.ok(statisticsService.getDeletedCommentsPercentage());
    }

    @GetMapping("/comments/recent")
    public ResponseEntity<Page<CommentResponse>> getRecentComments(Pageable pageable) {
        return ResponseEntity.ok(statisticsService.getRecentComments(pageable));
    }

    // === СТАТИСТИКА ПО РЕЙТИНГАМ ===

    @GetMapping("/ratings/total-count")
    public ResponseEntity<Long> getTotalRatingsCount() {
        return ResponseEntity.ok(statisticsService.getTotalRatingsCount());
    }

    @GetMapping("/ratings/global-average")
    public ResponseEntity<Double> getGlobalAverageRating() {
        return ResponseEntity.ok(statisticsService.getGlobalAverageRating());
    }

    @GetMapping("/ratings/distribution")
    public ResponseEntity<List<RatingDistributionDTO>> getGlobalRatingDistribution() {
        return ResponseEntity.ok(statisticsService.getGlobalRatingDistribution());
    }

    @GetMapping("/ratings/distribution/book/{bookId}")
    public ResponseEntity<List<RatingDistributionDTO>> getRatingDistributionByBook(
            @PathVariable Integer bookId) {
        return ResponseEntity.ok(statisticsService.getRatingDistributionByBook(bookId));
    }

    // === СТАТИСТИКА ПО ЗАКЛАДКАМ ===

    @GetMapping("/bookmarks/active-count")
    public ResponseEntity<Long> getActiveBookmarksCount() {
        return ResponseEntity.ok(statisticsService.getActiveBookmarksCount());
    }

    @GetMapping("/bookmarks/average-per-user")
    public ResponseEntity<Double> getAverageBookmarksPerUser() {
        return ResponseEntity.ok(statisticsService.getAverageBookmarksPerUser());
    }

    @GetMapping("/bookmarks/most-bookmarked-books")
    public ResponseEntity<Page<BookmarkStatDTO>> getMostBookmarkedBooksDetailed(Pageable pageable) {
        return ResponseEntity.ok(statisticsService.getMostBookmarkedBooksDetailed(pageable));
    }

    // === АКТИВНОСТЬ ПО ДАТАМ ===

    @GetMapping("/activity/comments")
    public ResponseEntity<List<ActivityByDateDTO>> getCommentActivityByDate(
            @RequestParam(required = false) LocalDateTime startDate) {
        LocalDateTime start = startDate != null ? startDate : LocalDateTime.now().minusDays(30);
        return ResponseEntity.ok(statisticsService.getCommentActivityByDate(start));
    }
}