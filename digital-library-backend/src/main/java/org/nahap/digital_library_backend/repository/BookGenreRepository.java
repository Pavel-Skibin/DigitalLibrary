package org.nahap.digital_library_backend.repository;

import org.nahap.digital_library_backend.entity.BookGenre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface BookGenreRepository extends JpaRepository<BookGenre, Integer> {

    List<BookGenre> findByBookId(Integer bookId);

    List<BookGenre> findByGenreId(Integer genreId);

    @Modifying
    @Transactional
    @Query("DELETE FROM BookGenre bg WHERE bg.book.id = :bookId")
    void deleteByBookId(@Param("bookId") Integer bookId);

    @Modifying
    @Transactional
    @Query("DELETE FROM BookGenre bg WHERE bg.genre.id = :genreId")
    void deleteByGenreId(@Param("genreId") Integer genreId);

    List<BookGenre> findByBookIdIn(List<Integer> bookIds);
}