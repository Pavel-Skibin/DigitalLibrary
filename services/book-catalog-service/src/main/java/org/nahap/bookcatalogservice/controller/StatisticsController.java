package org.nahap.bookcatalogservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nahap.bookcatalogservice.client.commentrating.model.ActiveCommenter;
import org.nahap.bookcatalogservice.client.commentrating.model.BookStatistics;
import org.nahap.bookcatalogservice.client.commentrating.model.RatingDistribution;
import org.nahap.bookcatalogservice.client.commentrating.model.RecentComment;
import org.nahap.bookcatalogservice.client.user.model.RoleStatistics;
import org.nahap.bookcatalogservice.dto.*;
import org.nahap.bookcatalogservice.service.StatisticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Public REST API for statistics aggregation
 */
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
@Slf4j
public class StatisticsController {

    private final StatisticsService statisticsService;

    /**
     * GET /api/statistics/system
     * Get system-wide statistics
     */
    @GetMapping("/system")
    public ResponseEntity<SystemStatisticsResponse> getSystemStatistics() {
        log.info("REST: GET /api/statistics/system");
        SystemStatisticsResponse stats = statisticsService.getSystemStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * GET /api/statistics/comments/deleted-percentage
     * Get percentage of deleted comments
     */
    @GetMapping("/comments/deleted-percentage")
    public ResponseEntity<Double> getDeletedCommentsPercentage() {
        log.info("REST: GET /api/statistics/comments/deleted-percentage");
        Double percentage = statisticsService.getDeletedCommentsPercentage();
        return ResponseEntity.ok(percentage);
    }

    /**
     * GET /api/statistics/ratings/distribution
     * Get rating distribution (1-5 stars)
     */
    @GetMapping("/ratings/distribution")
    public ResponseEntity<List<RatingDistribution>> getRatingDistribution() {
        log.info("REST: GET /api/statistics/ratings/distribution");
        List<RatingDistribution> distribution = statisticsService.getRatingDistribution();
        return ResponseEntity.ok(distribution);
    }

    /**
     * GET /api/statistics/users/top-commenters
     * Get most active commenters
     */
    @GetMapping("/users/top-commenters")
    public ResponseEntity<PageResponse<ActiveCommenterResponse>> getTopCommenters(
            @RequestParam(required = false, defaultValue = "5") Integer size
    ) {
        log.info("REST: GET /api/statistics/users/top-commenters?size={}", size);
        PageResponse<ActiveCommenterResponse> commenters = statisticsService.getMostActiveCommentersFormatted(size);
        return ResponseEntity.ok(commenters);
    }

    /**
     * GET /api/statistics/users/most-active-commenters
     * Get most active commenters (alternative endpoint)
     */
    @GetMapping("/users/most-active-commenters")
    public ResponseEntity<PageResponse<ActiveCommenterResponse>> getMostActiveCommenters(
            @RequestParam(required = false, defaultValue = "5") Integer size
    ) {
        log.info("REST: GET /api/statistics/users/most-active-commenters?size={}", size);
        PageResponse<ActiveCommenterResponse> commenters = statisticsService.getMostActiveCommentersFormatted(size);
        return ResponseEntity.ok(commenters);
    }

    /**
     * GET /api/statistics/comments/recent
     * Get recent comments
     */
    @GetMapping("/comments/recent")
    public ResponseEntity<PageResponse<RecentCommentResponse>> getRecentComments(
            @RequestParam(required = false, defaultValue = "5") Integer size
    ) {
        log.info("REST: GET /api/statistics/comments/recent?size={}", size);
        PageResponse<RecentCommentResponse> comments = statisticsService.getRecentCommentsFormatted(size);
        return ResponseEntity.ok(comments);
    }

    /**
     * GET /api/statistics/books/top-rated
     * Get top-rated books
     */
    @GetMapping("/books/top-rated")
    public ResponseEntity<PageResponse<TopRatedBookResponse>> getTopRatedBooks(
            @RequestParam(required = false, defaultValue = "3") Integer minRatings,
            @RequestParam(required = false, defaultValue = "5") Integer size
    ) {
        log.info("REST: GET /api/statistics/books/top-rated?minRatings={}&size={}", minRatings, size);
        PageResponse<TopRatedBookResponse> books = statisticsService.getTopRatedBooks(minRatings, size);
        return ResponseEntity.ok(books);
    }

    /**
     * GET /api/statistics/genres/top-by-count
     * Get top genres by book count
     */
    @GetMapping("/genres/top-by-count")
    public ResponseEntity<PageResponse<TopGenreResponse>> getTopGenresByCount(
            @RequestParam(required = false, defaultValue = "5") Integer size
    ) {
        log.info("REST: GET /api/statistics/genres/top-by-count?size={}", size);
        PageResponse<TopGenreResponse> genres = statisticsService.getTopGenresByBookCount(size);
        return ResponseEntity.ok(genres);
    }

    /**
     * GET /api/statistics/authors/top-by-count
     * Get top authors by book count
     */
    @GetMapping("/authors/top-by-count")
    public ResponseEntity<PageResponse<TopAuthorResponse>> getTopAuthorsByCount(
            @RequestParam(required = false, defaultValue = "5") Integer size
    ) {
        log.info("REST: GET /api/statistics/authors/top-by-count?size={}", size);
        PageResponse<TopAuthorResponse> authors = statisticsService.getTopAuthorsByBookCount(size);
        return ResponseEntity.ok(authors);
    }

    /**
     * GET /api/statistics/users/by-role
     * Get user statistics by role
     */
    @GetMapping("/users/by-role")
    public ResponseEntity<List<RoleStatistics>> getUsersByRole() {
        log.info("REST: GET /api/statistics/users/by-role");
        List<RoleStatistics> roleStats = statisticsService.getUsersByRole();
        return ResponseEntity.ok(roleStats);
    }

    /**
     * GET /api/statistics/books/all
     * Get all books statistics (used for cache synchronization)
     */
    @GetMapping("/books/all")
    public ResponseEntity<List<BookStatistics>> getAllBooksStatistics() {
        log.info("REST: GET /api/statistics/books/all");
        List<BookStatistics> stats = statisticsService.getAllBooksStatistics();
        return ResponseEntity.ok(stats);
    }
}
