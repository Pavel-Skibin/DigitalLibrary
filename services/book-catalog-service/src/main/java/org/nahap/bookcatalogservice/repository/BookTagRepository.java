package org.nahap.bookcatalogservice.repository;

import org.nahap.bookcatalogservice.entity.BookTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookTagRepository extends JpaRepository<BookTag, Integer> {

    @Modifying
    @Query("DELETE FROM BookTag bt WHERE bt.book.id = :bookId")
    void deleteByBookId(Integer bookId);

    @Query("SELECT bt FROM BookTag bt JOIN FETCH bt.tag WHERE bt.book.id IN :bookIds")
    List<BookTag> findByBookIdIn(List<Integer> bookIds);
}
