package org.nahap.digital_library_backend.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.nahap.digital_library_backend.entity.Genre;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@Tag("db")
@DisplayName("GenreRepository Tests with Real Data")
@Sql(scripts = "/sql/complete-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class GenreRepositoryWithDataTest extends BaseRepositoryTest {

    @Autowired
    private GenreRepository genreRepository;

    @Test
    @DisplayName("Should find 'Фантастика' genre by name")
    void shouldFindGenreByName_Fantasy() {
        Optional<Genre> genre = genreRepository.findByName("Фантастика");
        assertThat(genre).isPresent();
        assertThat(genre.get().getId()).isEqualTo(5);
        assertThat(genre.get().getName()).isEqualTo("Фантастика");
    }

    @Test
    @DisplayName("Should find 'Детектив' genre by name")
    void shouldFindGenreByName_Detective() {
        Optional<Genre> genre = genreRepository.findByName("Детектив");
        assertThat(genre).isPresent();
        assertThat(genre.get().getId()).isEqualTo(4);
    }

    @Test
    @DisplayName("Should return empty when genre name does not exist")
    void shouldReturnEmptyWhenGenreNotFound() {
        Optional<Genre> genre = genreRepository.findByName("Биография");
        assertThat(genre).isEmpty();
    }

    @Test
    @DisplayName("Should be case-sensitive: 'фантастика' should not match 'Фантастика'")
    void shouldBeCaseSensitive() {
        Optional<Genre> genre = genreRepository.findByName("фантастика");
        assertThat(genre).isEmpty();
    }
}