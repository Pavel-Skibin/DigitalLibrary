package org.nahap.bookcatalogservice.repository;

import org.nahap.bookcatalogservice.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Integer> {

    @Query("SELECT c FROM Comment c " +
           "WHERE c.book.id = :bookId AND c.deletedAt IS NULL " +
           "ORDER BY c.createdAt DESC")
    List<Comment> findActiveByBookId(@Param("bookId") Integer bookId);

    @EntityGraph(attributePaths = {"user", "book"})
    @Query("SELECT c FROM Comment c " +
           "WHERE c.book.id = :bookId AND c.deletedAt IS NULL " +
           "ORDER BY c.createdAt DESC")
    Page<Comment> findActiveByBookId(@Param("bookId") Integer bookId, Pageable pageable);

    @Query("SELECT c FROM Comment c " +
           "WHERE c.user.id = :userId AND c.deletedAt IS NULL")
    List<Comment> findActiveByUserId(@Param("userId") Integer userId);

    @Query("SELECT c FROM Comment c " +
           "WHERE c.user.id = :userId AND c.book.id = :bookId AND c.deletedAt IS NULL")
    Optional<Comment> findActiveByUserAndBook(
            @Param("userId") Integer userId,
            @Param("bookId") Integer bookId);

    @Query("SELECT c FROM Comment c " +
           "WHERE c.id = :id AND c.deletedAt IS NULL")
    Optional<Comment> findActiveById(@Param("id") Integer id);


    @EntityGraph(attributePaths = {"user", "book"})
    @Query("SELECT c FROM Comment c WHERE c.book.id = :bookId ORDER BY c.createdAt DESC")
    List<Comment> findAllByBookId(@Param("bookId") Integer bookId);

    @Query("SELECT c FROM Comment c WHERE c.user.id = :userId")
    List<Comment> findAllByUserId(@Param("userId") Integer userId);


    @EntityGraph(attributePaths = {"user", "book"})
    @Query("SELECT c FROM Comment c WHERE c.id = :id")
    Optional<Comment> findByIdWithRelations(@Param("id") Integer id);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.deletedAt IS NULL")
    Long countActiveComments();

    @Query("SELECT COUNT(c) FROM Comment c")
    Long countTotalComments();

    @Query("SELECT " +
           "CAST(COUNT(CASE WHEN c.deletedAt IS NOT NULL THEN 1 END) AS double) / COUNT(c) * 100 " +
           "FROM Comment c")
    Double getDeletedCommentsPercentage();

    @EntityGraph(attributePaths = {"user", "book"})
    @Query("SELECT c FROM Comment c " +
           "WHERE c.deletedAt IS NULL " +
           "ORDER BY c.createdAt DESC")
    Page<Comment> findRecentComments(Pageable pageable);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.book.id = :bookId AND c.deletedAt IS NULL")
    Long countActiveCommentsByBookId(@Param("bookId") Integer bookId);


}