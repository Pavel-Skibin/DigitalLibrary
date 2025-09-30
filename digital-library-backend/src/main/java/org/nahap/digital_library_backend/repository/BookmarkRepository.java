package org.nahap.digital_library_backend.repository;

import org.nahap.digital_library_backend.entity.Bookmark;
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

    @Query("SELECT b FROM Bookmark b " +
           "LEFT JOIN FETCH b.user " +
           "LEFT JOIN FETCH b.book " +
           "WHERE b.user.id = :userId AND b.deletedAt IS NULL")
    List<Bookmark> findActiveByUserId(@Param("userId") Integer userId);

    @Query(value = "SELECT b FROM Bookmark b " +
                   "LEFT JOIN FETCH b.user " +
                   "LEFT JOIN FETCH b.book " +
                   "WHERE b.user.id = :userId AND b.deletedAt IS NULL",
            countQuery = "SELECT COUNT(b) FROM Bookmark b WHERE b.user.id = :userId AND b.deletedAt IS NULL")
    Page<Bookmark> findActiveByUserId(@Param("userId") Integer userId, Pageable pageable);

    @Query("SELECT b FROM Bookmark b " +
           "LEFT JOIN FETCH b.user " +
           "LEFT JOIN FETCH b.book " +
           "WHERE b.user.id = :userId AND b.book.id = :bookId AND b.deletedAt IS NULL")
    Optional<Bookmark> findActiveByUserAndBook(
            @Param("userId") Integer userId,
            @Param("bookId") Integer bookId);

    @Query("SELECT b FROM Bookmark b " +
           "LEFT JOIN FETCH b.user " +
           "LEFT JOIN FETCH b.book " +
           "WHERE b.id = :id AND b.deletedAt IS NULL")
    Optional<Bookmark> findActiveById(@Param("id") Integer id);

    @Query(value = "SELECT b FROM Bookmark b " +
                   "LEFT JOIN FETCH b.user " +
                   "LEFT JOIN FETCH b.book " +
                   "WHERE b.user.id = :userId AND b.book.id = :bookId AND b.deletedAt IS NULL " +
                   "ORDER BY b.position ASC",
            countQuery = "SELECT COUNT(b) FROM Bookmark b " +
                         "WHERE b.user.id = :userId AND b.book.id = :bookId AND b.deletedAt IS NULL")
    Page<Bookmark> findActiveByUserIdAndBookId(
            @Param("userId") Integer userId,
            @Param("bookId") Integer bookId,
            Pageable pageable);

    @Query("SELECT COUNT(b) FROM Bookmark b WHERE b.deletedAt IS NULL")
    Long countActiveBookmarks();

    @Query("SELECT AVG(cnt) FROM " +
           "(SELECT COUNT(b) as cnt FROM Bookmark b " +
           "WHERE b.deletedAt IS NULL " +
           "GROUP BY b.user.id)")
    Double getAverageBookmarksPerUser();

    @Query("SELECT b.book, COUNT(b) as bookmarkCount " +
           "FROM Bookmark b " +
           "WHERE b.deletedAt IS NULL " +
           "GROUP BY b.book.id " +
           "ORDER BY bookmarkCount DESC")
    Page<Object[]> findMostBookmarkedBooks(Pageable pageable);

    @Query("SELECT COUNT(b) FROM Bookmark b WHERE b.book.id = :bookId AND b.deletedAt IS NULL")
    Long countActiveBookmarksByBookId(@Param("bookId") Integer bookId);
}