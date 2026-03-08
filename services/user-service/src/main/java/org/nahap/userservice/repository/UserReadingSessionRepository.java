package org.nahap.userservice.repository;

import org.nahap.userservice.entity.UserReadingSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserReadingSessionRepository extends JpaRepository<UserReadingSession, Long> {

    /**
     * Получить активные сессии пользователя (не завершенные)
     */
    List<UserReadingSession> findByUserIdAndEndedAtIsNullOrderByStartedAtDesc(Integer userId);

    /**
     * Получить активную сессию для конкретной книги
     */
    Optional<UserReadingSession> findByUserIdAndBookIdAndEndedAtIsNull(Integer userId, Integer bookId);

    /**
     * История сессий чтения пользователя
     */
    Page<UserReadingSession> findByUserIdOrderByStartedAtDesc(Integer userId, Pageable pageable);

    /**
     * Получить значимые сессии пользователя (для рекомендаций)
     */
    @Query("SELECT s FROM UserReadingSession s WHERE s.userId = :userId AND s.isSignificant = true ORDER BY s.startedAt DESC")
    List<UserReadingSession> findSignificantSessionsByUserId(@Param("userId") Integer userId);

    /**
     * Получить последние N значимых сессий (для ML)
     */
    @Query("SELECT s FROM UserReadingSession s WHERE s.userId = :userId AND s.isSignificant = true ORDER BY s.startedAt DESC")
    List<UserReadingSession> findTopSignificantSessions(@Param("userId") Integer userId, Pageable pageable);

    /**
     * Общее время чтения пользователя (в секундах)
     */
    @Query("SELECT COALESCE(SUM(s.durationSeconds), 0) FROM UserReadingSession s WHERE s.userId = :userId AND s.durationSeconds IS NOT NULL")
    Long getTotalReadingTimeByUserId(@Param("userId") Integer userId);

    /**
     * Количество завершенных сессий пользователя
     */
    Long countByUserIdAndEndedAtIsNotNull(Integer userId);

    /**
     * ID книг из значимых сессий (для рекомендаций)
     */
    @Query("SELECT DISTINCT s.bookId FROM UserReadingSession s WHERE s.userId = :userId AND s.isSignificant = true ORDER BY MAX(s.startedAt) DESC")
    List<Integer> findDistinctBookIdsFromSignificantSessions(@Param("userId") Integer userId);
}
