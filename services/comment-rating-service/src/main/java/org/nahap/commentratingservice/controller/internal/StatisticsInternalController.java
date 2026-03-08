package org.nahap.commentratingservice.controller.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nahap.commentratingservice.api.internal.InternalStatisticsApiApi;
import org.nahap.commentratingservice.api.internal.model.BookStatistics;
import org.nahap.commentratingservice.repository.BookmarkRepository;
import org.nahap.commentratingservice.repository.CommentRepository;
import org.nahap.commentratingservice.repository.RatingRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Internal API Controller for aggregated Book statistics
 * Implements OpenAPI generated interface
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class StatisticsInternalController implements InternalStatisticsApiApi {

    private final RatingRepository ratingRepository;
    private final CommentRepository commentRepository;
    private final BookmarkRepository bookmarkRepository;

    @Override
    public ResponseEntity<BookStatistics> getBookStatistics(Integer bookId) {
        log.debug("Internal API: Getting aggregated statistics for book: {}", bookId);
        
        Double averageRating = ratingRepository.findAverageRatingByBookId(bookId);
        Long ratingsCount = ratingRepository.countRatingsByBookId(bookId);
        Long commentsCount = commentRepository.countActiveCommentsByBookId(bookId);
        Long bookmarksCount = bookmarkRepository.countActiveBookmarksByBookId(bookId);
        
        BookStatistics statistics = new BookStatistics();
        statistics.setBookId(bookId);
        statistics.setAverageRating(averageRating != null ? averageRating : 0.0);
        statistics.setRatingsCount(ratingsCount);
        statistics.setCommentsCount(commentsCount);
        statistics.setBookmarksCount(bookmarksCount);
        
        return ResponseEntity.ok(statistics);
    }

    @Override
    public ResponseEntity<List<BookStatistics>> getAllBooksStatistics() {
        log.info("Internal API: Getting statistics for all books with ratings");
        
        List<Integer> bookIds = ratingRepository.findAllDistinctBookIds();
        log.debug("Found {} books with ratings", bookIds.size());
        
        List<BookStatistics> statisticsList = bookIds.stream()
                .map(bookId -> {
                    Double averageRating = ratingRepository.findAverageRatingByBookId(bookId);
                    Long ratingsCount = ratingRepository.countRatingsByBookId(bookId);
                    Long commentsCount = commentRepository.countActiveCommentsByBookId(bookId);
                    Long bookmarksCount = bookmarkRepository.countActiveBookmarksByBookId(bookId);
                    
                    BookStatistics statistics = new BookStatistics();
                    statistics.setBookId(bookId);
                    statistics.setAverageRating(averageRating != null ? averageRating : 0.0);
                    statistics.setRatingsCount(ratingsCount);
                    statistics.setCommentsCount(commentsCount);
                    statistics.setBookmarksCount(bookmarksCount);
                    
                    return statistics;
                })
                .toList();
        
        log.info("Returning statistics for {} books", statisticsList.size());
        return ResponseEntity.ok(statisticsList);
    }

    @Override
    public ResponseEntity<Long> getTotalRatingsCount() {
        log.debug("Internal API: Getting total ratings count");
        Long count = ratingRepository.count();
        return ResponseEntity.ok(count);
    }

    @Override
    public ResponseEntity<Long> getTotalCommentsCount() {
        log.debug("Internal API: Getting total comments count");
        Long count = commentRepository.count();
        return ResponseEntity.ok(count);
    }

    @Override
    public ResponseEntity<Double> getDeletedCommentsPercentage() {
        log.debug("Internal API: Getting deleted comments percentage");
        Long totalCount = commentRepository.count();
        Long deletedCount = commentRepository.countDeletedComments();
        
        double percentage = totalCount > 0 ? (deletedCount * 100.0) / totalCount : 0.0;
        return ResponseEntity.ok(percentage);
    }

    @Override
    public ResponseEntity<List<org.nahap.commentratingservice.api.internal.model.RatingDistribution>> getRatingDistribution() {
        log.debug("Internal API: Getting rating distribution");
        
        List<org.nahap.commentratingservice.api.internal.model.RatingDistribution> distribution = new java.util.ArrayList<>();
        
        for (int i = 1; i <= 5; i++) {
            Long count = ratingRepository.countByRatingValue(i);
            org.nahap.commentratingservice.api.internal.model.RatingDistribution item = 
                new org.nahap.commentratingservice.api.internal.model.RatingDistribution();
            item.setRatingValue(i);
            item.setCount(count);
            distribution.add(item);
        }
        
        return ResponseEntity.ok(distribution);
    }

    @Override
    public ResponseEntity<List<org.nahap.commentratingservice.api.internal.model.ActiveCommenter>> getMostActiveCommenters(Integer size) {
        log.debug("Internal API: Getting most active commenters, size={}", size);
        
        int limit = size != null ? size : 5;
        org.springframework.data.domain.Pageable pageable = 
            org.springframework.data.domain.PageRequest.of(0, limit);
        List<Object[]> results = commentRepository.findMostActiveCommenters(pageable);
        
        List<org.nahap.commentratingservice.api.internal.model.ActiveCommenter> commenters = results.stream()
                .map(row -> {
                    org.nahap.commentratingservice.api.internal.model.ActiveCommenter commenter = 
                        new org.nahap.commentratingservice.api.internal.model.ActiveCommenter();
                    commenter.setUserId((Integer) row[0]);
                    commenter.setUsername("User#" + row[0]); // Placeholder - Book Catalog will enrich with real username
                    commenter.setCommentCount(((Number) row[1]).longValue());
                    return commenter;
                })
                .toList();
        
        return ResponseEntity.ok(commenters);
    }

    @Override
    public ResponseEntity<List<org.nahap.commentratingservice.api.internal.model.RecentComment>> getRecentComments(Integer size) {
        log.debug("Internal API: Getting recent comments, size={}", size);
        
        int limit = size != null ? size : 5;
        List<org.nahap.commentratingservice.entity.Comment> comments = 
            commentRepository.findRecentComments(org.springframework.data.domain.PageRequest.of(0, limit));
        
        List<org.nahap.commentratingservice.api.internal.model.RecentComment> recentComments = comments.stream()
                .map(comment -> {
                    org.nahap.commentratingservice.api.internal.model.RecentComment recent = 
                        new org.nahap.commentratingservice.api.internal.model.RecentComment();
                    recent.setId(comment.getId());
                    recent.setBookId(comment.getBookId());
                    recent.setUserId(comment.getUserId());
                    recent.setUsername("User#" + comment.getUserId()); // Placeholder
                    recent.setCommentText(comment.getText());
                    // Convert LocalDateTime to OffsetDateTime
                    recent.setCreatedAt(comment.getCreatedAt().atOffset(java.time.ZoneOffset.UTC));
                    return recent;
                })
                .toList();
        
        return ResponseEntity.ok(recentComments);
    }

    @Override
    public ResponseEntity<Double> getGlobalAverageRating() {
        log.debug("Internal API: Getting global average rating");
        Double avgRating = ratingRepository.findGlobalAverageRating();
        return ResponseEntity.ok(avgRating != null ? avgRating : 0.0);
    }
}
