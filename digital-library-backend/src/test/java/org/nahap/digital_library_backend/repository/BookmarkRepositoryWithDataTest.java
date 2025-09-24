package org.nahap.digital_library_backend.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.nahap.digital_library_backend.entity.Bookmark;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@Tag("db")
@DisplayName("BookmarkRepository Tests with Real Data")
@Sql(scripts = "/sql/complete-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class BookmarkRepositoryWithDataTest extends BaseRepositoryTest {

    @Autowired
    private BookmarkRepository bookmarkRepository;

    @Test
    @DisplayName("Should find active bookmarks for user ID 1 (LiamSmith)")
    void shouldFindActiveBookmarksByUserId() {
        List<Bookmark> bookmarks = bookmarkRepository.findActiveByUserId(1);
        assertThat(bookmarks).hasSize(3);
        assertThat(bookmarks).extracting(Bookmark::getName)
                .contains("Детство Зулали", "Первая улика", "Приезд в деревню");
    }

    @Test
    @DisplayName("Should paginate bookmarks for user ID 4 (OliviaBrown)")
    void shouldPaginateBookmarks() {
        var page = bookmarkRepository.findActiveByUserId(4, PageRequest.of(0, 2));
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should find bookmark by user and book (LiamSmith + Зулали)")
    void shouldFindBookmarkByUserAndBook() {
        Optional<Bookmark> bookmark = bookmarkRepository.findActiveByUserAndBook(1, 1);
        assertThat(bookmark).isPresent();
        assertThat(bookmark.get().getPosition()).isEqualTo(0.25);
        assertThat(bookmark.get().getName()).isEqualTo("Детство Зулали");
    }

    @Test
    @DisplayName("Should return empty for non-existing bookmark")
    void shouldReturnEmptyForNonExistingBookmark() {
        Optional<Bookmark> bookmark = bookmarkRepository.findActiveByUserAndBook(999, 999);
        assertThat(bookmark).isEmpty();
    }
}