package org.nahap.bookcatalogservice.repository;

import org.nahap.bookcatalogservice.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StatisticsRepository extends JpaRepository<Book, Integer> {

    // Общая статистика системы (одним запросом)
    @Query("SELECT " +
           "COUNT(DISTINCT b.id) as totalBooks, " +
           "COUNT(DISTINCT a.id) as totalAuthors, " +
           "COUNT(DISTINCT g.id) as totalGenres, " +
           "COUNT(DISTINCT u.id) as totalUsers " +
           "FROM Book b, Author a, Genre g, User u " +
           "WHERE u.deletedAt IS NULL")
    Object[] getSystemOverview();

    // Активность по датам (количество комментариев за последние N дней)
    @Query("SELECT CAST(c.createdAt AS date), COUNT(c) " +
           "FROM Comment c " +
           "WHERE c.deletedAt IS NULL AND c.createdAt >= :startDate " +
           "GROUP BY CAST(c.createdAt AS date) " +
           "ORDER BY CAST(c.createdAt AS date) DESC")
    List<Object[]> getCommentActivityByDate(@Param("startDate") LocalDateTime startDate);
}