package org.nahap.commentratingservice.repository;

import org.nahap.commentratingservice.entity.Bookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Integer> {

    @Query("SELECT b FROM Bookmark b WHERE b.userId = :userId AND b.deletedAt IS NULL")
    List<Bookmark> findActiveByUserId(@Param("userId") Integer userId, Pageable pageable);

    @Query("SELECT b FROM Bookmark b WHERE b.userId = :userId AND b.bookId = :bookId AND b.deletedAt IS NULL")
    Optional<Bookmark> findActiveByUserAndBook(
            @Param("userId") Integer userId,
            @Param("bookId") Integer bookId);

    @Query("SELECT b FROM Bookmark b WHERE b.id = :id AND b.deletedAt IS NULL")
    Optional<Bookmark> findActiveById(@Param("id") Integer id);

    @Query(value = "SELECT b FROM Bookmark b " +
                   "WHERE b.userId = :userId AND b.bookId = :bookId AND b.deletedAt IS NULL " +
                   "ORDER BY b.position ASC",
            countQuery = "SELECT COUNT(b) FROM Bookmark b " +
                         "WHERE b.userId = :userId AND b.bookId = :bookId AND b.deletedAt IS NULL")
    Page<Bookmark> findActiveByUserIdAndBookId(
            @Param("userId") Integer userId,
            @Param("bookId") Integer bookId,
            Pageable pageable);

    @Query("SELECT COUNT(b) FROM Bookmark b WHERE b.deletedAt IS NULL")
    Long countActiveBookmarks();

    @Query("SELECT COUNT(b) FROM Bookmark b WHERE b.bookId = :bookId AND b.deletedAt IS NULL")
    Long countActiveBookmarksByBookId(@Param("bookId") Integer bookId);
}
