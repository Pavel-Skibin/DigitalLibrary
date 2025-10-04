// src/main/java/org/nahap/digital_library_backend/service/impl/StatisticsServiceImpl.java
package org.nahap.digital_library_backend.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nahap.digital_library_backend.dto.mapper.*;
import org.nahap.digital_library_backend.dto.response.*;
import org.nahap.digital_library_backend.entity.*;
import org.nahap.digital_library_backend.exception.BookNotFoundException;
import org.nahap.digital_library_backend.repository.*;
import org.nahap.digital_library_backend.service.StatisticsService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsServiceImpl implements StatisticsService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final GenreRepository genreRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final RatingRepository ratingRepository;
    private final BookmarkRepository bookmarkRepository;
    private final StatisticsRepository statisticsRepository;

    private final BookMapper bookMapper;
    private final CommentMapper commentMapper;

    // === Общая статистика системы ===

    @Override
    public SystemStatisticsDTO getSystemStatistics() {
        Long totalBooks = bookRepository.countTotalBooks();
        Long totalAuthors = authorRepository.count();
        Long totalGenres = genreRepository.count();
        Long totalUsers = userRepository.countActiveUsers();
        Long totalRatings = ratingRepository.countTotalRatings();
        Long totalComments = commentRepository.countActiveComments();
        Double globalAverageRating = ratingRepository.getGlobalAverageRating();

        return new SystemStatisticsDTO(
                totalBooks,
                totalAuthors,
                totalGenres,
                totalUsers,
                totalRatings,
                totalComments,
                globalAverageRating != null ? Math.round(globalAverageRating * 10.0) / 10.0 : 0.0
        );
    }

    // === Статистика по книгам ===

    @Override
    public Page<BookStatDTO> getTopRatedBooks(Long minRatings, Pageable pageable) {
        Page<Book> booksPage = bookRepository.findTopRatedBooks(minRatings != null ? minRatings : 1L, pageable);

        List<Integer> bookIds = booksPage.getContent().stream()
                .map(Book::getId)
                .toList();

        Map<Integer, Double> avgRatings = loadAverageRatingsByBookId(bookIds);
        Map<Integer, Long> ratingCounts = loadRatingCountsByBookId(bookIds);
        Map<Integer, Long> commentCounts = loadCommentCountsByBookId(bookIds);
        Map<Integer, Long> bookmarkCounts = loadBookmarkCountsByBookId(bookIds);

        List<BookStatDTO> stats = booksPage.getContent().stream()
                .map(book -> new BookStatDTO(
                        book.getId(),
                        book.getTitle(),
                        avgRatings.getOrDefault(book.getId(), 0.0),
                        ratingCounts.getOrDefault(book.getId(), 0L),
                        commentCounts.getOrDefault(book.getId(), 0L),
                        bookmarkCounts.getOrDefault(book.getId(), 0L)
                ))
                .toList();

        return new PageImpl<>(stats, pageable, booksPage.getTotalElements());
    }

    @Override
    public Page<BookStatDTO> getMostCommentedBooks(Pageable pageable) {
        Page<Book> booksPage = bookRepository.findMostCommentedBooks(pageable);
        return mapToBooksStatDTO(booksPage, pageable);
    }

    @Override
    public Page<BookStatDTO> getMostBookmarkedBooks(Pageable pageable) {
        Page<Book> booksPage = bookRepository.findMostBookmarkedBooks(pageable);
        return mapToBooksStatDTO(booksPage, pageable);
    }

    @Override
    public List<BookResponse> getBooksWithoutRatings() {
        List<Book> books = bookRepository.findBooksWithoutRatings();
        return books.stream()
                .map(bookMapper::toResponse)
                .toList();
    }

    @Override
    public Long getTotalBooksCount() {
        return bookRepository.countTotalBooks();
    }

    @Override
    public Long getBookCountByGenre(Integer genreId) {
        return genreRepository.countBooksByGenre(genreId);
    }

    @Override
    public Long getBookCountByAuthor(Integer authorId) {
        return authorRepository.countBooksByAuthor(authorId);
    }

    // === Статистика по жанрам ===

    @Override
    public Page<GenreStatDTO> getTopGenresByBookCount(Pageable pageable) {
        Page<Object[]> results = genreRepository.findTopGenresByBookCount(pageable);

        List<GenreStatDTO> stats = results.getContent().stream()
                .map(row -> {
                    Integer genreId = ((Number) row[0]).intValue();     // ID
                    String genreName = (String) row[1];                   // Название
                    Long bookCount = ((Number) row[2]).longValue();       // Количество
                    return new GenreStatDTO(
                            genreId,
                            genreName,
                            bookCount,
                            null
                    );
                })
                .toList();

        return new PageImpl<>(stats, pageable, results.getTotalElements());
    }

    @Override
    public Page<GenreStatDTO> getTopGenresByRating(Long minRatings, Pageable pageable) {
        Page<Object[]> results = genreRepository.findTopGenresByRating(
                minRatings != null ? minRatings : 1L,
                pageable
        );

        List<GenreStatDTO> stats = results.getContent().stream()
                .map(row -> {
                    Integer genreId = ((Number) row[0]).intValue();
                    String genreName = (String) row[1];
                    Double avgRating = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
                    return new GenreStatDTO(
                            genreId,
                            genreName,
                            null,
                            Math.round(avgRating * 10.0) / 10.0
                    );
                })
                .toList();

        return new PageImpl<>(stats, pageable, results.getTotalElements());
    }
    // === Статистика по авторам ===

    @Override
    public Page<AuthorStatDTO> getTopAuthorsByBookCount(Pageable pageable) {
        Page<Object[]> results = authorRepository.findTopAuthorsByBookCount(pageable);

        List<AuthorStatDTO> stats = results.getContent().stream()
                .map(row -> {
                    Integer authorId = ((Number) row[0]).intValue();
                    String firstName = (String) row[1];
                    String lastName = (String) row[2];
                    Long bookCount = ((Number) row[3]).longValue();
                    return new AuthorStatDTO(
                            authorId,
                            firstName,
                            lastName,
                            firstName + " " + lastName,
                            bookCount,
                            null
                    );
                })
                .toList();

        return new PageImpl<>(stats, pageable, results.getTotalElements());
    }

    @Override
    public Page<AuthorStatDTO> getTopAuthorsByRating(Long minRatings, Pageable pageable) {
        Page<Object[]> results = authorRepository.findTopAuthorsByRating(
                minRatings != null ? minRatings : 1L,
                pageable
        );

        List<AuthorStatDTO> stats = results.getContent().stream()
                .map(row -> {
                    Integer authorId = ((Number) row[0]).intValue();
                    String firstName = (String) row[1];
                    String lastName = (String) row[2];
                    Double avgRating = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;
                    return new AuthorStatDTO(
                            authorId,
                            firstName,
                            lastName,
                            firstName + " " + lastName,
                            null,
                            Math.round(avgRating * 10.0) / 10.0
                    );
                })
                .toList();

        return new PageImpl<>(stats, pageable, results.getTotalElements());
    }

    // === Статистика по пользователям ===

    @Override
    public Long getActiveUsersCount() {
        return userRepository.countActiveUsers();
    }

    @Override
    public List<UserRoleCountDTO> getUserCountByRole() {
        List<Object[]> results = userRepository.countUsersByRole();
        return results.stream()
                .map(row -> new UserRoleCountDTO(
                        (String) row[0],
                        ((Number) row[1]).longValue()
                ))
                .toList();
    }

    @Override
    public Page<UserActivityDTO> getMostActiveCommenters(Pageable pageable) {
        Page<Object[]> results = userRepository.findMostActiveCommenters(pageable);

        List<UserActivityDTO> stats = results.getContent().stream()
                .map(row -> {
                    Integer userId = ((Number) row[0]).intValue();
                    String username = (String) row[1];
                    Long count = ((Number) row[2]).longValue();
                    return new UserActivityDTO(
                            userId,
                            username,
                            count,
                            "comments"
                    );
                })
                .toList();

        return new PageImpl<>(stats, pageable, results.getTotalElements());
    }

    @Override
    public Page<UserActivityDTO> getMostActiveRaters(Pageable pageable) {
        Page<Object[]> results = userRepository.findMostActiveRaters(pageable);

        List<UserActivityDTO> stats = results.getContent().stream()
                .map(row -> {
                    Integer userId = ((Number) row[0]).intValue();
                    String username = (String) row[1];
                    Long count = ((Number) row[2]).longValue();
                    return new UserActivityDTO(
                            userId,
                            username,
                            count,
                            "ratings"
                    );
                })
                .toList();

        return new PageImpl<>(stats, pageable, results.getTotalElements());
    }

    // === Статистика по комментариям ===

    @Override
    public Long getActiveCommentsCount() {
        return commentRepository.countActiveComments();
    }

    @Override
    public Long getTotalCommentsCount() {
        return commentRepository.countTotalComments();
    }

    @Override
    public Double getDeletedCommentsPercentage() {
        Double percentage = commentRepository.getDeletedCommentsPercentage();
        return percentage != null ? Math.round(percentage * 10.0) / 10.0 : 0.0;
    }

    @Override
    public Page<CommentResponse> getRecentComments(Pageable pageable) {
        Page<Comment> comments = commentRepository.findRecentComments(pageable);
        return comments.map(commentMapper::toResponse);
    }

    // === Статистика по рейтингам ===

    @Override
    public Long getTotalRatingsCount() {
        return ratingRepository.countTotalRatings();
    }

    @Override
    public Double getGlobalAverageRating() {
        Double avg = ratingRepository.getGlobalAverageRating();
        return avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0;
    }

    @Override
    public List<RatingDistributionDTO> getGlobalRatingDistribution() {
        List<Object[]> results = ratingRepository.getRatingDistribution();
        return results.stream()
                .map(row -> new RatingDistributionDTO(
                        ((Number) row[0]).intValue(),
                        ((Number) row[1]).longValue()
                ))
                .toList();
    }

    @Override
    public List<RatingDistributionDTO> getRatingDistributionByBook(Integer bookId) {
        if (!bookRepository.existsById(bookId)) {
            throw new BookNotFoundException("Книга с ID " + bookId + " не найдена");
        }

        List<Object[]> results = ratingRepository.getRatingDistributionByBook(bookId);
        return results.stream()
                .map(row -> new RatingDistributionDTO(
                        ((Number) row[0]).intValue(),
                        ((Number) row[1]).longValue()
                ))
                .toList();
    }

    // === Статистика по закладкам ===

    @Override
    public Long getActiveBookmarksCount() {
        return bookmarkRepository.countActiveBookmarks();
    }

    @Override
    public Double getAverageBookmarksPerUser() {
        Double avg = bookmarkRepository.getAverageBookmarksPerUser();
        return avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0;
    }

    @Override
    public Page<BookmarkStatDTO> getMostBookmarkedBooksDetailed(Pageable pageable) {
        Page<Object[]> results = bookmarkRepository.findMostBookmarkedBooks(pageable);

        List<BookmarkStatDTO> stats = results.getContent().stream()
                .map(row -> {
                    Book book = (Book) row[0];
                    Long count = ((Number) row[1]).longValue();
                    return new BookmarkStatDTO(
                            book.getId(),
                            book.getTitle(),
                            count
                    );
                })
                .toList();

        return new PageImpl<>(stats, pageable, results.getTotalElements());
    }

    // === Активность по датам ===

    @Override
    public List<ActivityByDateDTO> getCommentActivityByDate(LocalDateTime startDate) {
        List<Object[]> results = statisticsRepository.getCommentActivityByDate(startDate);
        return results.stream()
                .map(row -> new ActivityByDateDTO(
                        row[0].toString(), // дата
                        ((Number) row[1]).longValue()
                ))
                .toList();
    }

    // === Вспомогательные методы ===

    private Page<BookStatDTO> mapToBooksStatDTO(Page<Book> booksPage, Pageable pageable) {
        List<Integer> bookIds = booksPage.getContent().stream()
                .map(Book::getId)
                .toList();

        Map<Integer, Double> avgRatings = loadAverageRatingsByBookId(bookIds);
        Map<Integer, Long> ratingCounts = loadRatingCountsByBookId(bookIds);
        Map<Integer, Long> commentCounts = loadCommentCountsByBookId(bookIds);
        Map<Integer, Long> bookmarkCounts = loadBookmarkCountsByBookId(bookIds);

        List<BookStatDTO> stats = booksPage.getContent().stream()
                .map(book -> new BookStatDTO(
                        book.getId(),
                        book.getTitle(),
                        avgRatings.getOrDefault(book.getId(), 0.0),
                        ratingCounts.getOrDefault(book.getId(), 0L),
                        commentCounts.getOrDefault(book.getId(), 0L),
                        bookmarkCounts.getOrDefault(book.getId(), 0L)
                ))
                .toList();

        return new PageImpl<>(stats, pageable, booksPage.getTotalElements());
    }

    private Map<Integer, Double> loadAverageRatingsByBookId(List<Integer> bookIds) {
        List<Rating> ratings = ratingRepository.findByBookIdIn(bookIds);
        return ratings.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getBook().getId(),
                        Collectors.averagingDouble(r -> r.getValue().doubleValue())
                ));
    }

    private Map<Integer, Long> loadRatingCountsByBookId(List<Integer> bookIds) {
        List<Rating> ratings = ratingRepository.findByBookIdIn(bookIds);
        return ratings.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getBook().getId(),
                        Collectors.counting()
                ));
    }

    private Map<Integer, Long> loadCommentCountsByBookId(List<Integer> bookIds) {
        return bookIds.stream()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> commentRepository.countActiveCommentsByBookId(id)
                ));
    }

    private Map<Integer, Long> loadBookmarkCountsByBookId(List<Integer> bookIds) {
        return bookIds.stream()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> bookmarkRepository.countActiveBookmarksByBookId(id)
                ));
    }


}