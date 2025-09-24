package org.nahap.digital_library_backend.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.nahap.digital_library_backend.entity.Author;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@Tag("db")
@DisplayName("AuthorRepository Tests with Real Data")
@Sql(scripts = "/sql/complete-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class AuthorRepositoryWithDataTest extends BaseRepositoryTest {

    @Autowired
    private AuthorRepository authorRepository;

    @Test
    @DisplayName("Should find authors by first name containing 'Наринэ' (case-insensitive)")
    void shouldFindAuthorsByFirstNameContainingIgnoreCase() {
        // when
        List<Author> authors = authorRepository.findByFirstNameContainingIgnoreCase("наринэ");

        // then
        assertThat(authors).hasSize(1);
        assertThat(authors.get(0).getFirstName()).isEqualTo("Наринэ");
        assertThat(authors.get(0).getLastName()).isEqualTo("Абгарян");
    }

    @Test
    @DisplayName("Should find authors by last name containing 'Абгарян' (case-insensitive)")
    void shouldFindAuthorsByLastNameContainingIgnoreCase() {
        // when
        List<Author> authors = authorRepository.findByLastNameContainingIgnoreCase("АБГАРЯН");

        // then
        assertThat(authors).hasSize(1);
        assertThat(authors.get(0).getFirstName()).isEqualTo("Наринэ");
        assertThat(authors.get(0).getLastName()).isEqualTo("Абгарян");
    }

    @Test
    @DisplayName("Should find authors by first name or last name containing 'Аристотель' (case-insensitive)")
    void shouldFindAuthorsByFirstNameOrLastNameContainingIgnoreCase_Aristotle() {
        // when
        List<Author> authors = authorRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
                "аристотель", "аристотель");

        // then
        assertThat(authors).hasSize(1);
        assertThat(authors.get(0).getFirstName()).isEqualTo("Аристотель");
        assertThat(authors.get(0).getLastName()).isEmpty();
    }

    @Test
    @DisplayName("Should find authors by first name 'Борис' or last name 'Акунин' (case-insensitive)")
    void shouldFindAuthorsByFirstNameOrLastNameContainingIgnoreCase_Akunin() {
        // when
        List<Author> authors = authorRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
                "борис", "акунин");

        // then
        assertThat(authors).hasSize(1);
        assertThat(authors.get(0).getFirstName()).isEqualTo("Борис");
        assertThat(authors.get(0).getLastName()).isEqualTo("Акунин");
    }

    @Test
    @DisplayName("Should return empty list when no author matches partial name")
    void shouldReturnEmptyListWhenNoMatch() {
        // when
        List<Author> authors = authorRepository.findByFirstNameContainingIgnoreCase("Неизвестный");

        // then
        assertThat(authors).isEmpty();
    }

    @Test
    @DisplayName("Should find multiple authors when search term matches several (e.g., 'а')")
    void shouldFindMultipleAuthorsWithCommonLetter() {
        // when
        List<Author> authors = authorRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase("а", "а");

        // then
        assertThat(authors).hasSize(7);
    }
}