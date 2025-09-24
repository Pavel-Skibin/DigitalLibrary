package org.nahap.digital_library_backend.repository;

import org.nahap.digital_library_backend.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Integer> {

    @Query("SELECT r FROM Rating r WHERE r.book.id = :bookId")
    List<Rating> findByBookId(@Param("bookId") Integer bookId);

    @Query("SELECT r FROM Rating r WHERE r.user.id = :userId")
    List<Rating> findByUserId(@Param("userId") Integer userId);

    @Query("SELECT r FROM Rating r WHERE r.user.id = :userId AND r.book.id = :bookId")
    Optional<Rating> findByUserAndBook(
            @Param("userId") Integer userId,
            @Param("bookId") Integer bookId);

    @Query("SELECT AVG(r.value) FROM Rating r WHERE r.book.id = :bookId")
    Double findAverageRatingByBookId(@Param("bookId") Integer bookId);

    @Query("SELECT COUNT(r) FROM Rating r WHERE r.book.id = :bookId")
    Long countRatingsByBookId(@Param("bookId") Integer bookId);
}