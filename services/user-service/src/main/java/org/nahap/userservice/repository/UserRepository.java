package org.nahap.userservice.repository;


import org.nahap.userservice.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.username = :username AND u.deletedAt IS NULL")
    Optional<User> findActiveByUsername(@Param("username") String username);

    @Query("SELECT u FROM User u WHERE u.id = :id AND u.deletedAt IS NULL")
    Optional<User> findActiveById(@Param("id") Integer id);

    @Query("SELECT u FROM User u WHERE u.email = :email AND u.deletedAt IS NULL")
    Optional<User> findActiveByEmail(@Param("email") String email);


    @Query("SELECT u FROM User u WHERE " +
           "(:query IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<User> searchUsers(@Param("query") String query, Pageable pageable);

    // Общее количество пользователей (активных)
    @Query("SELECT COUNT(u) FROM User u WHERE u.deletedAt IS NULL")
    Long countActiveUsers();

    // Получить всех активных пользователей
    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL")
    List<User> findAllActive();

    // Количество пользователей по ролям
    @Query("SELECT u.role.name, COUNT(u) FROM User u " +
           "WHERE u.deletedAt IS NULL " +
           "GROUP BY u.role.name")
    List<Object[]> countUsersByRole();

    // Проверка существования по username
    boolean existsByUsername(String username);

    // Проверка существования по email
    boolean existsByEmail(String email);
}