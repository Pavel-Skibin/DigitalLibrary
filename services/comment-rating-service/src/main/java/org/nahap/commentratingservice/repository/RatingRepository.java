package org.nahap.commentratingservice.repository;

import org.nahap.commentratingservice.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Integer> {

    @Query("SELECT r FROM Rating r WHERE r.bookId = :bookId")
    List<Rating> findByBookId(@Param("bookId") Integer bookId);

    @Query("SELECT r FROM Rating r WHERE r.userId = :userId")
    List<Rating> findByUserId(@Param("userId") Integer userId);

    @Query("SELECT r FROM Rating r WHERE r.userId = :userId AND r.bookId = :bookId")
    Optional<Rating> findByUserAndBook(
            @Param("userId") Integer userId,
            @Param("bookId") Integer bookId);

    @Query("SELECT AVG(r.value) FROM Rating r WHERE r.bookId = :bookId")
    Double findAverageRatingByBookId(@Param("bookId") Integer bookId);

    @Query("SELECT COUNT(r) FROM Rating r WHERE r.bookId = :bookId")
    Long countRatingsByBookId(@Param("bookId") Integer bookId);

    @Query("SELECT COUNT(r) FROM Rating r")
    Long countTotalRatings();

    @Query("SELECT AVG(r.value) FROM Rating r")
    Double getGlobalAverageRating();

    @Query("SELECT r.value, COUNT(r) FROM Rating r WHERE r.bookId = :bookId GROUP BY r.value ORDER BY r.value")
    List<Object[]> getRatingDistributionByBook(@Param("bookId") Integer bookId);
}
