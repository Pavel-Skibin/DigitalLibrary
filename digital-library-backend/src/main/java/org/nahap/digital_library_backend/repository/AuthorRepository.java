package org.nahap.digital_library_backend.repository;

import org.nahap.digital_library_backend.entity.Author;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.util.List;

@Repository
public interface AuthorRepository extends JpaRepository<Author, Integer> {


    List<Author> findByFirstNameContainingIgnoreCase(String firstName);
    List<Author> findByLastNameContainingIgnoreCase(String lastName);
    Page<Author> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
            String firstName, String lastName, Pageable pageable);

    @Query("SELECT CASE WHEN COUNT(ba) > 0 THEN true ELSE false END FROM BookAuthor ba WHERE ba.author.id = :authorId")
    boolean hasBooks(@Param("authorId") Integer authorId);
    @Query("SELECT COUNT(DISTINCT ba.book.id) FROM BookAuthor ba WHERE ba.author.id = :authorId")
    Long countBooksByAuthorId(@Param("authorId") Integer authorId);
    @Query("SELECT COUNT(DISTINCT ba.book.id) FROM BookAuthor ba WHERE ba.author.id = :authorId")
    Long countBooksByAuthor(@Param("authorId") Integer authorId);

    @Query("SELECT ba.author.id, ba.author.firstName, ba.author.lastName, COUNT(DISTINCT ba.book.id) as bookCount " +
           "FROM BookAuthor ba " +
           "GROUP BY ba.author.id, ba.author.firstName, ba.author.lastName " +
           "ORDER BY bookCount DESC")
    Page<Object[]> findTopAuthorsByBookCount(Pageable pageable);

    @Query("SELECT ba.author.id, ba.author.firstName, ba.author.lastName, AVG(r.value) as avgRating " +
           "FROM BookAuthor ba " +
           "LEFT JOIN ba.book.ratings r " +
           "GROUP BY ba.author.id, ba.author.firstName, ba.author.lastName " +
           "HAVING COUNT(r) >= :minRatings " +
           "ORDER BY avgRating DESC")
    Page<Object[]> findTopAuthorsByRating(@Param("minRatings") Long minRatings, Pageable pageable);

}