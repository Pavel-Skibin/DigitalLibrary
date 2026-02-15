package org.nahap.bookcatalogservice.repository;

import org.nahap.bookcatalogservice.entity.BookAuthor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface BookAuthorRepository extends JpaRepository<BookAuthor, Integer> {

    List<BookAuthor> findByBookId(Integer bookId);

    List<BookAuthor> findByAuthorId(Integer authorId);

    @Modifying
    @Transactional
    @Query("DELETE FROM BookAuthor ba WHERE ba.book.id = :bookId")
    void deleteByBookId(@Param("bookId") Integer bookId);

    @Modifying
    @Transactional
    @Query("DELETE FROM BookAuthor ba WHERE ba.author.id = :authorId")
    void deleteByAuthorId(@Param("authorId") Integer authorId);

    List<BookAuthor> findByBookIdIn(List<Integer> bookIds);

}