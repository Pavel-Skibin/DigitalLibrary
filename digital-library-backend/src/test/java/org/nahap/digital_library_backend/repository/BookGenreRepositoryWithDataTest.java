package org.nahap.digital_library_backend.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.nahap.digital_library_backend.entity.BookGenre;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@Tag("db")
@DisplayName("BookGenreRepository Tests with Real Data")
@Sql(scripts = "/sql/complete-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class BookGenreRepositoryWithDataTest extends BaseRepositoryTest {

    @Autowired
    private BookGenreRepository bookGenreRepository;

    @Test
    @DisplayName("Should find all BookGenre links for book ID 1 ('Зулали')")
    void shouldFindBookGenresByBookId() {
        List<BookGenre> links = bookGenreRepository.findByBookId(1);
        assertThat(links).hasSize(3);
        assertThat(links).extracting(l -> l.getGenre().getName())
                .contains("Современная проза", "Драма", "Юмор");
    }

    @Test
    @DisplayName("Should find all BookGenre links for genre ID 5 (Фантастика)")
    void shouldFindBookGenresByGenreId() {
        List<BookGenre> links = bookGenreRepository.findByGenreId(5);
        assertThat(links).hasSize(2); // книги 4 и 7
        assertThat(links)
                .extracting(l -> l.getBook().getTitle())
                .contains(
                        "Четвертый ледниковый период",
                        "Сокровища Валькирии. Стоящий у Солнца"
                );
    }

    @Test
    @DisplayName("Delete by genre ID should work")
    void deleteByGenreIdShouldWork() {
        assertThat(bookGenreRepository.findByGenreId(5)).hasSize(2);
        bookGenreRepository.deleteByGenreId(5);
        assertThat(bookGenreRepository.findByGenreId(5)).isEmpty();
    }
}