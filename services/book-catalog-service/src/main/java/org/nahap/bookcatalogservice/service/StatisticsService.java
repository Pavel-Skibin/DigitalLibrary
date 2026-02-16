package org.nahap.bookcatalogservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nahap.bookcatalogservice.client.CommentRatingInternalClient;
import org.nahap.bookcatalogservice.client.UserDataInternalClient;
import org.nahap.bookcatalogservice.client.UserInternalClient;
import org.nahap.bookcatalogservice.client.commentrating.model.ActiveCommenter;
import org.nahap.bookcatalogservice.client.commentrating.model.BookStatistics;
import org.nahap.bookcatalogservice.client.commentrating.model.RatingDistribution;
import org.nahap.bookcatalogservice.client.commentrating.model.RecentComment;
import org.nahap.bookcatalogservice.client.user.model.RoleStatistics;
import org.nahap.bookcatalogservice.client.user.model.UserResponse;
import org.nahap.bookcatalogservice.dto.*;
import org.nahap.bookcatalogservice.entity.Author;
import org.nahap.bookcatalogservice.entity.Book;
import org.nahap.bookcatalogservice.entity.Genre;
import org.nahap.bookcatalogservice.repository.AuthorRepository;
import org.nahap.bookcatalogservice.repository.BookRepository;
import org.nahap.bookcatalogservice.repository.GenreRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for aggregating statistics from multiple microservices
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StatisticsService {

    private final CommentRatingInternalClient commentRatingClient;
    private final UserInternalClient userClient;
    private final UserDataInternalClient userDataClient;
    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final GenreRepository genreRepository;

    /**
     * Get system-wide statistics aggregated from all services
     */
    public SystemStatisticsResponse getSystemStatistics() {
        log.info("Fetching system statistics from multiple services");

        try {
            // Local queries
            Long totalBooks = bookRepository.count();
            Long totalAuthors = authorRepository.count();
            Long totalGenres = genreRepository.count();

            // Feign queries to Comment Rating Service
            Long totalComments = extractBody(commentRatingClient.getTotalCommentsCount(), 0L);
            Long totalRatings = extractBody(commentRatingClient.getTotalRatingsCount(), 0L);
            Double globalAverageRating = extractBody(commentRatingClient.getGlobalAverageRating(), 0.0);

            // Feign query to User Service
            Long totalUsers = extractBody(userClient.getTotalUsersCount(), 0L);

            log.info("System statistics: books={}, users={}, authors={}, genres={}, avgRating={}, comments={}, ratings={}",
                    totalBooks, totalUsers, totalAuthors, totalGenres, globalAverageRating, totalComments, totalRatings);

            return new SystemStatisticsResponse(
                    totalBooks,
                    totalUsers,
                    totalAuthors,
                    totalGenres,
                    globalAverageRating,
                    totalComments,
                    totalRatings
            );
        } catch (Exception e) {
            log.error("Error fetching system statistics", e);
            throw new RuntimeException("Failed to fetch system statistics", e);
        }
    }

    /**
     * Get deleted comments percentage
     */
    public Double getDeletedCommentsPercentage() {
        log.info("Fetching deleted comments percentage");
        return extractBody(commentRatingClient.getDeletedCommentsPercentage(), 0.0);
    }

    /**
     * Get rating distribution (1-5 stars)
     */
    public List<RatingDistribution> getRatingDistribution() {
        log.info("Fetching rating distribution");
        return extractBody(commentRatingClient.getRatingDistribution(), List.of());
    }

    /**
     * Get most active commenters
     */
    public List<ActiveCommenter> getMostActiveCommenters(Integer size) {
        log.info("Fetching most active commenters with size={}", size);
        return extractBody(commentRatingClient.getMostActiveCommenters(size), List.of());
    }

    /**
     * Get recent comments
     */
    public List<RecentComment> getRecentComments(Integer size) {
        log.info("Fetching recent comments with size={}", size);
        return extractBody(commentRatingClient.getRecentComments(size), List.of());
    }

    /**
     * Get user statistics by role
     */
    public List<RoleStatistics> getUsersByRole() {
        log.info("Fetching users by role");
        return extractBody(userClient.getUsersByRole(), List.of());
    }

    /**
     * Get all books statistics (for caching)
     */
    public List<BookStatistics> getAllBooksStatistics() {
        log.info("Fetching all books statistics");
        return extractBody(commentRatingClient.getAllBooksStatistics(), List.of());
    }

    /**
     * Get top-rated books with titles
     */
    public PageResponse<TopRatedBookResponse> getTopRatedBooks(Integer minRatings, Integer size) {
        log.info("Fetching top rated books with minRatings={}, size={}", minRatings, size);
        
        // Get all book statistics from Comment Rating Service
        List<BookStatistics> allStats = extractBody(commentRatingClient.getAllBooksStatistics(), List.of());
        
        // Filter by minimum ratings and sort by average rating
        List<BookStatistics> filtered = allStats.stream()
                .filter(stat -> stat.getRatingsCount() >= (minRatings != null ? minRatings : 0))
                .sorted(Comparator.comparing(BookStatistics::getAverageRating).reversed())
                .limit(size != null ? size : 5)
                .toList();
        
        // Get book IDs and fetch titles
        List<Integer> bookIds = filtered.stream().map(BookStatistics::getBookId).toList();
        Map<Integer, String> bookTitles = bookRepository.findAllById(bookIds).stream()
                .collect(Collectors.toMap(Book::getId, Book::getTitle));
        
        // Map to response DTOs
        List<TopRatedBookResponse> response = filtered.stream()
                .map(stat -> new TopRatedBookResponse(
                        stat.getBookId(),
                        bookTitles.getOrDefault(stat.getBookId(), "Unknown"),
                        stat.getAverageRating(),
                        stat.getRatingsCount(),
                        stat.getCommentsCount()
                ))
                .toList();
        
        return PageResponse.of(response);
    }

    /**
     * Get top genres by book count
     */
    public PageResponse<TopGenreResponse> getTopGenresByBookCount(Integer size) {
        log.info("Fetching top genres by book count with size={}", size);
        
        int limit = size != null ? size : 5;
        Pageable pageable = PageRequest.of(0, limit);
        Page<Object[]> results = genreRepository.findTopGenresByBookCount(pageable);
        
        List<TopGenreResponse> response = results.getContent().stream()
                .map(row -> new TopGenreResponse(
                        (Integer) row[0],  // genreId
                        (String) row[1],   // name
                        ((Number) row[2]).longValue()  // bookCount
                ))
                .toList();
        
        return PageResponse.of(response);
    }

    /**
     * Get top authors by book count
     */
    public PageResponse<TopAuthorResponse> getTopAuthorsByBookCount(Integer size) {
        log.info("Fetching top authors by book count with size={}", size);
        
        int limit = size != null ? size : 5;
        Pageable pageable = PageRequest.of(0, limit);
        Page<Object[]> results = authorRepository.findTopAuthorsByBookCount(pageable);
        
        List<TopAuthorResponse> response = results.getContent().stream()
                .map(row -> new TopAuthorResponse(
                        (Integer) row[0],  // authorId
                        row[1] + " " + row[2],  // firstName + lastName
                        ((Number) row[3]).longValue()  // bookCount
                ))
                .toList();
        
        return PageResponse.of(response);
    }

    /**
     * Get most active commenters (formatted for frontend)
     */
    public PageResponse<ActiveCommenterResponse> getMostActiveCommentersFormatted(Integer size) {
        log.info("Fetching most active commenters formatted with size={}", size);
        
        List<ActiveCommenter> commenters = extractBody(commentRatingClient.getMostActiveCommenters(size), List.of());
        
        // Enrich with real usernames from User Service
        Map<Integer, String> usernames = fetchUsernames(
                commenters.stream().map(ActiveCommenter::getUserId).toList()
        );
        
        List<ActiveCommenterResponse> response = commenters.stream()
                .map(c -> new ActiveCommenterResponse(
                        c.getUserId(),
                        usernames.getOrDefault(c.getUserId(), "User#" + c.getUserId()),
                        c.getCommentCount()  // maps to activityCount in DTO
                ))
                .toList();
        
        return PageResponse.of(response);
    }

    /**
     * Get recent comments (formatted for frontend)
     */
    public PageResponse<RecentCommentResponse> getRecentCommentsFormatted(Integer size) {
        log.info("Fetching recent comments formatted with size={}", size);
        
        List<RecentComment> comments = extractBody(commentRatingClient.getRecentComments(size), List.of());
        
        // Enrich with real usernames from User Service
        Map<Integer, String> usernames = fetchUsernames(
                comments.stream().map(RecentComment::getUserId).toList()
        );
        
        List<RecentCommentResponse> response = comments.stream()
                .map(c -> new RecentCommentResponse(
                        c.getId(),
                        c.getBookId(),
                        c.getUserId(),
                        usernames.getOrDefault(c.getUserId(), "User#" + c.getUserId()),  // maps to userName in DTO
                        c.getCommentText(),  // maps to text in DTO
                        c.getCreatedAt()
                ))
                .toList();
        
        return PageResponse.of(response);
    }

    /**
     * Fetch usernames from User Service using batch API
     */
    private Map<Integer, String> fetchUsernames(List<Integer> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        
        try {
            ResponseEntity<Map<String, UserResponse>> response = userDataClient.getUsersBatch(userIds);
            if (response != null && response.getBody() != null) {
                return response.getBody().entrySet().stream()
                        .collect(Collectors.toMap(
                                entry -> Integer.parseInt(entry.getKey()),
                                entry -> entry.getValue().getUsername()
                        ));
            }
        } catch (Exception e) {
            log.error("Failed to fetch usernames from User Service", e);
        }
        
        return Map.of();
    }

    /**
     * Extract body from ResponseEntity with fallback value
     */
    private <T> T extractBody(ResponseEntity<T> response, T fallback) {
        if (response != null && response.getBody() != null) {
            return response.getBody();
        }
        log.warn("Empty response received, returning fallback value");
        return fallback;
    }
}
