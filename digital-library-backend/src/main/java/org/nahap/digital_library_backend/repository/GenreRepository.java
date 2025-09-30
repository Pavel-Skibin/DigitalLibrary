package org.nahap.digital_library_backend.repository;

import org.nahap.digital_library_backend.entity.Genre;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GenreRepository extends JpaRepository<Genre, Integer> {

    Optional<Genre> findByName(String name);

    @Query("SELECT CASE WHEN COUNT(bg) > 0 THEN true ELSE false END FROM BookGenre bg WHERE bg.genre.id = :genreId")
    boolean hasBooks(@Param("genreId") Integer genreId);

    @Query("SELECT COUNT(DISTINCT bg.book.id) FROM BookGenre bg WHERE bg.genre.id = :genreId")
    Long countBooksByGenre(@Param("genreId") Integer genreId);

    @Query("SELECT bg.genre.id, bg.genre.name, COUNT(DISTINCT bg.book.id) as bookCount " +
           "FROM BookGenre bg " +
           "GROUP BY bg.genre.id, bg.genre.name " +
           "ORDER BY bookCount DESC")
    Page<Object[]> findTopGenresByBookCount(Pageable pageable);

    @Query("SELECT bg.genre.id, bg.genre.name, AVG(r.value) as avgRating " +
           "FROM BookGenre bg " +
           "LEFT JOIN bg.book.ratings r " +
           "GROUP BY bg.genre.id, bg.genre.name " +
           "HAVING COUNT(r) >= :minRatings " +
           "ORDER BY avgRating DESC")
    Page<Object[]> findTopGenresByRating(@Param("minRatings") Long minRatings, Pageable pageable);
}