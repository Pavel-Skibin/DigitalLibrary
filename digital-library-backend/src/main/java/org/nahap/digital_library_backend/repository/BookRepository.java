package org.nahap.digital_library_backend.repository;

import org.nahap.digital_library_backend.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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

    Page<Book> findAll(Pageable pageable);

//    // Для BookDetailResponse — с предзагрузкой связей
//    @Query("SELECT b FROM Book b " +
//           "JOIN FETCH b.bookAuthors ba " +
//           "JOIN FETCH ba.author " +
//           "JOIN FETCH b.bookGenres bg " +
//           "JOIN FETCH bg.genre " +
//           "LEFT JOIN FETCH b.ratings " +
//           "LEFT JOIN FETCH b.comments " + // ← Без условия!
//           "WHERE b.id = :id")
//    Optional<Book> findDetailedById(@Param("id") Integer id);
}