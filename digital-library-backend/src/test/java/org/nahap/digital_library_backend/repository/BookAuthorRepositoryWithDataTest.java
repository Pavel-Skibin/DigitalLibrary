package org.nahap.digital_library_backend.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.nahap.digital_library_backend.entity.BookAuthor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@Tag("db")
@DisplayName("BookAuthorRepository Tests with Real Data")
@Sql(scripts = "/sql/complete-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class BookAuthorRepositoryWithDataTest extends BaseRepositoryTest {

    @Autowired
    private BookAuthorRepository bookAuthorRepository;

    @Test
    @DisplayName("Should find all BookAuthor links for book ID 1 ('Зулали')")
    void shouldFindBookAuthorsByBookId() {
        List<BookAuthor> links = bookAuthorRepository.findByBookId(1);
        assertThat(links).hasSize(1);
        assertThat(links.get(0).getAuthor().getFirstName()).isEqualTo("Наринэ");
    }

    @Test
    @DisplayName("Should find all BookAuthor links for author ID 7 (Аристотель)")
    void shouldFindBookAuthorsByAuthorId() {
        List<BookAuthor> links = bookAuthorRepository.findByAuthorId(7);
        assertThat(links).hasSize(2); // Метафизика и Политика
        assertThat(links).extracting(l -> l.getBook().getTitle())
                .contains("Метафизика", "Политика");
    }

    @Test
    @DisplayName("Delete by book ID should work (tested via count before/after)")
    void deleteByBookIdShouldWork() {
        assertThat(bookAuthorRepository.findByBookId(1)).hasSize(1);
        bookAuthorRepository.deleteByBookId(1);
        assertThat(bookAuthorRepository.findByBookId(1)).isEmpty();
    }
}