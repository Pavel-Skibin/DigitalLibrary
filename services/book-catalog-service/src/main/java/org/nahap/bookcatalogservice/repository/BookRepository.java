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

    // Простой поиск с динамической сортировкой (через Pageable)
    @Query("SELECT DISTINCT b FROM Book b " +
           "LEFT JOIN b.bookAuthors ba " +
           "LEFT JOIN b.bookGenres bg " +
           "WHERE (:title IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :title, '%'))) " +
           "AND (:authorIds IS NULL OR ba.author.id IN :authorIds) " +
           "AND (:genreIds IS NULL OR bg.genre.id IN :genreIds)")
    Page<Book> searchBooksSimpleWithSort(
            @Param("title") String title,
            @Param("authorIds") List<Integer> authorIds,
            @Param("genreIds") List<Integer> genreIds,
            Pageable pageable
    );

    @Query("SELECT COUNT(b) FROM Book b")
    Long countTotalBooks();

    @Query("SELECT COUNT(DISTINCT bg.book.id) FROM BookGenre bg WHERE bg.genre.id = :genreId")
    Long countBooksByGenre(@Param("genreId") Integer genreId);

    @Query("SELECT COUNT(DISTINCT ba.book.id) FROM BookAuthor ba WHERE ba.author.id = :authorId")
    Long countBooksByAuthor(@Param("authorId") Integer authorId);

}