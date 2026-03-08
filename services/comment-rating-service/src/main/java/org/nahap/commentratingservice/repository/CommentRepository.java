package org.nahap.commentratingservice.repository;

import org.nahap.commentratingservice.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Integer> {

    @Query("SELECT c FROM Comment c " +
           "WHERE c.bookId = :bookId AND c.deletedAt IS NULL " +
           "ORDER BY c.createdAt DESC")
    Page<Comment> findActiveByBookId(@Param("bookId") Integer bookId, Pageable pageable);

    @Query("SELECT c FROM Comment c " +
           "WHERE c.userId = :userId AND c.deletedAt IS NULL")
    List<Comment> findActiveByUserId(@Param("userId") Integer userId);

    @Query("SELECT c FROM Comment c " +
           "WHERE c.userId = :userId AND c.bookId = :bookId AND c.deletedAt IS NULL")
    Optional<Comment> findActiveByUserAndBook(
            @Param("userId") Integer userId,
            @Param("bookId") Integer bookId);

    @Query("SELECT c FROM Comment c " +
           "WHERE c.id = :id AND c.deletedAt IS NULL")
    Optional<Comment> findActiveById(@Param("id") Integer id);

    @Query("SELECT c FROM Comment c WHERE c.bookId = :bookId ORDER BY c.createdAt DESC")
    List<Comment> findAllByBookId(@Param("bookId") Integer bookId);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.deletedAt IS NULL")
    Long countActiveComments();

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.bookId = :bookId AND c.deletedAt IS NULL")
    Long countActiveCommentsByBookId(@Param("bookId") Integer bookId);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.deletedAt IS NOT NULL")
    Long countDeletedComments();

    @Query("SELECT c.userId, COUNT(c) " +
           "FROM Comment c " +
           "WHERE c.deletedAt IS NULL " +
           "GROUP BY c.userId " +
           "ORDER BY COUNT(c) DESC")
    List<Object[]> findMostActiveCommenters(Pageable pageable);

    @Query("SELECT c FROM Comment c " +
           "WHERE c.deletedAt IS NULL " +
           "ORDER BY c.createdAt DESC")
    List<Comment> findRecentComments(Pageable pageable);
}
