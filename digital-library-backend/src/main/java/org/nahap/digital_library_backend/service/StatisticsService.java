// src/main/java/org/nahap/digital_library_backend/service/StatisticsService.java
package org.nahap.digital_library_backend.service;

import org.nahap.digital_library_backend.dto.response.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface StatisticsService {

    // === Общая статистика системы ===
    SystemStatisticsDTO getSystemStatistics();

    // === Статистика по книгам ===
    Page<BookStatDTO> getTopRatedBooks(Long minRatings, Pageable pageable);
    Page<BookStatDTO> getMostCommentedBooks(Pageable pageable);
    Page<BookStatDTO> getMostBookmarkedBooks(Pageable pageable);
    List<BookResponse> getBooksWithoutRatings();
    Long getTotalBooksCount();
    Long getBookCountByGenre(Integer genreId);
    Long getBookCountByAuthor(Integer authorId);

    // === Статистика по жанрам ===
    Page<GenreStatDTO> getTopGenresByBookCount(Pageable pageable);
    Page<GenreStatDTO> getTopGenresByRating(Long minRatings, Pageable pageable);

    // === Статистика по авторам ===
    Page<AuthorStatDTO> getTopAuthorsByBookCount(Pageable pageable);
    Page<AuthorStatDTO> getTopAuthorsByRating(Long minRatings, Pageable pageable);

    // === Статистика по пользователям ===
    Long getActiveUsersCount();
    List<UserRoleCountDTO> getUserCountByRole();
    Page<UserActivityDTO> getMostActiveCommenters(Pageable pageable);
    Page<UserActivityDTO> getMostActiveRaters(Pageable pageable);

    // === Статистика по комментариям ===
    Long getActiveCommentsCount();
    Long getTotalCommentsCount();
    Double getDeletedCommentsPercentage();
    Page<CommentResponse> getRecentComments(Pageable pageable);

    // === Статистика по рейтингам ===
    Long getTotalRatingsCount();
    Double getGlobalAverageRating();
    List<RatingDistributionDTO> getGlobalRatingDistribution();
    List<RatingDistributionDTO> getRatingDistributionByBook(Integer bookId);

    // === Статистика по закладкам ===
    Long getActiveBookmarksCount();
    Double getAverageBookmarksPerUser();
    Page<BookmarkStatDTO> getMostBookmarkedBooksDetailed(Pageable pageable);

    // === Активность по датам ===
    List<ActivityByDateDTO> getCommentActivityByDate(LocalDateTime startDate);
}