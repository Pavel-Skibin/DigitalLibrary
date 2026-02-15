package org.nahap.bookcatalogservice.repository;

import org.nahap.bookcatalogservice.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Integer> {

    List<Book> findByTitleContainingIgnoreCase(String title);

    @Query("SELECT DISTINCT b FROM Book b " +
           "JOIN b.bookGenres bg " +
           "WHERE bg.genre.id = :genreId")
    List<Book> findByGenreId(@Param("genreId") Integer genreId);

    @Query("SELECT DISTINCT b FROM Book b " +
           "JOIN b.bookAuthors ba " +
           "WHERE ba.author.id = :authorId")
    List<Book> findByAuthorId(@Param("authorId") Integer authorId);

    @Query("SELECT DISTINCT b FROM Book b " +
           "LEFT JOIN b.bookAuthors ba " +
           "LEFT JOIN b.bookGenres bg " +
           "WHERE (:title IS NULL OR LOWER(CAST(b.title AS string)) LIKE LOWER(CONCAT('%', :title, '%'))) " +
           "AND (:authorIds IS NULL OR ba.author.id IN :authorIds) " +
           "AND (:genreIds IS NULL OR bg.genre.id IN :genreIds)")
    Page<Book> searchBooksSimple(
            @Param("title") String title,
            @Param("authorIds") List<Integer> authorIds,
            @Param("genreIds") List<Integer> genreIds,
            Pageable pageable
    );

    // 1. Простой поиск с сортировкой по названию (ASC)
    @Query("SELECT DISTINCT b FROM Book b " +
           "LEFT JOIN b.bookAuthors ba " +
           "LEFT JOIN b.bookGenres bg " +
           "WHERE (:title IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :title, '%'))) " +
           "AND (:authorIds IS NULL OR ba.author.id IN :authorIds) " +
           "AND (:genreIds IS NULL OR bg.genre.id IN :genreIds) " +
           "ORDER BY b.title ASC")
    Page<Book> searchBooksSimpleWithSort(
            @Param("title") String title,
            @Param("authorIds") List<Integer> authorIds,
            @Param("genreIds") List<Integer> genreIds,
            Pageable pageable
    );

    // 2. Поиск с агрегацией и сортировкой (по рейтингу или по названию)

    @Query("SELECT b FROM Book b " +
           "LEFT JOIN b.bookAuthors ba " +
           "LEFT JOIN b.bookGenres bg " +
           "LEFT JOIN b.ratings r " +
           "WHERE (:title IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :title, '%'))) " +
           "AND (:authorIds IS NULL OR ba.author.id IN :authorIds) " +
           "AND (:genreIds IS NULL OR bg.genre.id IN :genreIds) " +
           "GROUP BY b.id, b.title " +
           "HAVING (:minRating IS NULL OR AVG(r.value) >= :minRating) " +
           "AND (:maxRating IS NULL OR AVG(r.value) <= :maxRating) " +
           "ORDER BY " +
           "   CASE WHEN :sortField = 'rating' THEN COALESCE(AVG(r.value), -1) END DESC, " +
           "   b.title ASC")
    Page<Book> searchBooksWithRatingAndSort(
            @Param("title") String title,
            @Param("authorIds") List<Integer> authorIds,
            @Param("genreIds") List<Integer> genreIds,
            @Param("minRating") Double minRating,
            @Param("maxRating") Double maxRating,
            @Param("sortField") String sortField,
            Pageable pageable
    );

    // Топ N самых популярных книг по среднему рейтингу (с минимальным количеством оценок)
    @Query("SELECT b FROM Book b " +
           "LEFT JOIN b.ratings r " +
           "GROUP BY b.id " +
           "HAVING COUNT(r) >= :minRatings " +
           "ORDER BY AVG(r.value) DESC")
    Page<Book> findTopRatedBooks(@Param("minRatings") Long minRatings, Pageable pageable);

    // Топ N книг по количеству комментариев
    @Query("SELECT b FROM Book b " +
           "LEFT JOIN b.comments c " +
           "WHERE c.deletedAt IS NULL " +
           "GROUP BY b.id " +
           "ORDER BY COUNT(c) DESC")
    Page<Book> findMostCommentedBooks(Pageable pageable);

    // Топ N книг по количеству закладок (самые читаемые)
    @Query("SELECT b FROM Book b " +
           "LEFT JOIN b.bookmarks bm " +
           "WHERE bm.deletedAt IS NULL " +
           "GROUP BY b.id " +
           "ORDER BY COUNT(bm) DESC")
    Page<Book> findMostBookmarkedBooks(Pageable pageable);


    @Query("SELECT COUNT(b) FROM Book b")
    Long countTotalBooks();


    @Query("SELECT COUNT(DISTINCT bg.book.id) FROM BookGenre bg WHERE bg.genre.id = :genreId")
    Long countBooksByGenre(@Param("genreId") Integer genreId);


    @Query("SELECT COUNT(DISTINCT ba.book.id) FROM BookAuthor ba WHERE ba.author.id = :authorId")
    Long countBooksByAuthor(@Param("authorId") Integer authorId);


    @Query("SELECT b FROM Book b " +
           "WHERE NOT EXISTS (SELECT r FROM Rating r WHERE r.book.id = b.id)")
    List<Book> findBooksWithoutRatings();


}