package org.nahap.userservice.repository;

import org.nahap.userservice.entity.UserBookFavorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserBookFavoriteRepository extends JpaRepository<UserBookFavorite, Long> {

    /**
     * Получить избранные книги пользователя (с пагинацией)
     */
    Page<UserBookFavorite> findByUserIdOrderByAddedAtDesc(Integer userId, Pageable pageable);

    /**
     * Получить список ID избранных книг пользователя
     */
    @Query("SELECT f.bookId FROM UserBookFavorite f WHERE f.userId = :userId ORDER BY f.addedAt DESC")
    List<Integer> findBookIdsByUserId(@Param("userId") Integer userId);

    /**
     * Проверить, в избранном ли книга у пользователя
     */
    boolean existsByUserIdAndBookId(Integer userId, Integer bookId);

    /**
     * Найти конкретную запись избранного
     */
    Optional<UserBookFavorite> findByUserIdAndBookId(Integer userId, Integer bookId);

    /**
     * Количество избранных книг у пользователя
     */
    Long countByUserId(Integer userId);

    /**
     * Количество пользователей, добавивших книгу в избранное
     */
    Long countByBookId(Integer bookId);

    /**
     * Удалить из избранного
     */
    void deleteByUserIdAndBookId(Integer userId, Integer bookId);
}
