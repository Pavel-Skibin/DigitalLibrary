package org.nahap.userservice.repository;

import org.nahap.userservice.entity.UserBookView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserBookViewRepository extends JpaRepository<UserBookView, Long> {

    /**
     * Получить историю просмотров пользователя (с пагинацией)
     */
    Page<UserBookView> findByUserIdOrderByViewedAtDesc(Integer userId, Pageable pageable);

    /**
     * Получить последние N просмотров пользователя
     */
    List<UserBookView> findTop10ByUserIdOrderByViewedAtDesc(Integer userId);

    /**
     * Проверить, просматривал ли пользователь книгу
     */
    boolean existsByUserIdAndBookId(Integer userId, Integer bookId);

    /**
     * Количество уникальных книг, просмотренных пользователем
     */
    @Query("SELECT COUNT(DISTINCT v.bookId) FROM UserBookView v WHERE v.userId = :userId")
    Long countDistinctBooksByUserId(@Param("userId") Integer userId);

    /**
     * Количество просмотров книги
     */
    Long countByBookId(Integer bookId);

    /**
     * Получить уникальные ID книг, просмотренных пользователем (для рекомендаций)
     */
    @Query("SELECT DISTINCT v.bookId FROM UserBookView v WHERE v.userId = :userId ORDER BY v.viewedAt DESC")
    List<Integer> findDistinctBookIdsByUserId(@Param("userId") Integer userId);

    /**
     * Найти запись просмотра для пары пользователь-книга
     */
    java.util.Optional<UserBookView> findByUserIdAndBookId(Integer userId, Integer bookId);

    /**
     * Получить топ книг по общему времени чтения для пользователя
     */
    @Query("SELECT v FROM UserBookView v WHERE v.userId = :userId AND v.totalReadingTimeSeconds > 0 " +
           "ORDER BY v.totalReadingTimeSeconds DESC")
    List<UserBookView> findTopBooksByReadingTime(@Param("userId") Integer userId, Pageable pageable);

    /**
     * Получить последние прочитанные книги пользователя
     */
    @Query("SELECT v FROM UserBookView v WHERE v.userId = :userId AND v.lastReadAt IS NOT NULL " +
           "ORDER BY v.lastReadAt DESC")
    List<UserBookView> findRecentlyReadBooks(@Param("userId") Integer userId, Pageable pageable);

    /**
     * Получить завершенные книги пользователя
     */
    List<UserBookView> findByUserIdAndIsCompletedTrue(Integer userId);

    /**
     * Удалить старые просмотры (для очистки)
     */
    void deleteByViewedAtBefore(LocalDateTime date);
}
