package org.nahap.digital_library_backend.repository;

import org.nahap.digital_library_backend.entity.User;
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

    @Query("SELECT u FROM User u WHERE u.email = :email AND u.deletedAt IS NULL")
    Optional<User> findActiveByEmail(@Param("email") String email);


    @Query("SELECT u FROM User u WHERE " +
           "(:query IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<User> searchUsers(@Param("query") String query, Pageable pageable);

    // Общее количество пользователей (активных)
    @Query("SELECT COUNT(u) FROM User u WHERE u.deletedAt IS NULL")
    Long countActiveUsers();

    // Количество пользователей по ролям
    @Query("SELECT u.role.name, COUNT(u) FROM User u " +
           "WHERE u.deletedAt IS NULL " +
           "GROUP BY u.role.name")
    List<Object[]> countUsersByRole();







    @Query("SELECT c.user.id, c.user.username, COUNT(c) as commentCount " +
           "FROM Comment c " +
           "WHERE c.deletedAt IS NULL " +
           "GROUP BY c.user.id, c.user.username " +
           "ORDER BY commentCount DESC")
    Page<Object[]> findMostActiveCommenters(Pageable pageable);

    @Query("SELECT r.user.id, r.user.username, COUNT(r) as ratingCount " +
           "FROM Rating r " +
           "GROUP BY r.user.id, r.user.username " +
           "ORDER BY ratingCount DESC")
    Page<Object[]> findMostActiveRaters(Pageable pageable);
}