package org.nahap.digital_library_backend.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.nahap.digital_library_backend.entity.Author;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.*;

@Tag("db")
@DisplayName("AuthorRepository Integration Tests")
@Sql(scripts = "/sql/complete-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class AuthorRepositoryIntegrationTest extends BaseRepositoryTest {

    @Autowired
    private AuthorRepository authorRepository;


    private Page<Author> searchAuthors(String query, int size) {
        return authorRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
                query, query, PageRequest.of(0, size)
        );
    }

    @Test
    @DisplayName("Should find authors by first name containing 'Наринэ' (case-insensitive)")
    void shouldFindAuthorsByFirstNameContainingIgnoreCase() {
        // when
        Page<Author> page = searchAuthors("наринэ", 10);

        // then
        assertThat(page.getContent()).hasSize(1);
        Author author = page.getContent().get(0);
        assertThat(author.getFirstName()).isEqualTo("Наринэ");
        assertThat(author.getLastName()).isEqualTo("Абгарян");
    }

    @Test
    @DisplayName("Should find authors by last name containing 'Абгарян' (case-insensitive)")
    void shouldFindAuthorsByLastNameContainingIgnoreCase() {
        // when
        Page<Author> page = searchAuthors("АБГАРЯН", 10);

        // then
        assertThat(page.getContent()).hasSize(1);
        Author author = page.getContent().get(0);
        assertThat(author.getFirstName()).isEqualTo("Наринэ");
        assertThat(author.getLastName()).isEqualTo("Абгарян");
    }

    @Test
    @DisplayName("Should find authors by first name or last name containing 'Аристотель' (case-insensitive)")
    void shouldFindAuthorsByFirstNameOrLastNameContainingIgnoreCase_Aristotle() {
        // when
        Page<Author> page = searchAuthors("аристотель", 10);

        // then
        assertThat(page.getContent()).hasSize(1);
        Author author = page.getContent().get(0);
        assertThat(author.getFirstName()).isEqualTo("Аристотель");
        assertThat(author.getLastName()).isEmpty();
    }

    @Test
    @DisplayName("Should find authors by first name 'Борис' or last name 'Акунин' (case-insensitive)")
    void shouldFindAuthorsByFirstNameOrLastNameContainingIgnoreCase_Akunin() {
        // when
        Page<Author> page = searchAuthors("борис", 10);

        // then
        assertThat(page.getContent()).hasSize(1);
        Author author = page.getContent().get(0);
        assertThat(author.getFirstName()).isEqualTo("Борис");
        assertThat(author.getLastName()).isEqualTo("Акунин");
    }

    @Test
    @DisplayName("Should return empty page when no author matches partial name")
    void shouldReturnEmptyPageWhenNoMatch() {
        // when
        Page<Author> page = searchAuthors("Неизвестный", 10);

        // then
        assertThat(page.getContent()).isEmpty();
    }

    @Test
    @DisplayName("Should find multiple authors when search term matches several (e.g., 'а')")
    void shouldFindMultipleAuthorsWithCommonLetter() {
        // when
        Page<Author> page = searchAuthors("а", 20); // увеличим размер страницы, чтобы вместить всех

        // then
        assertThat(page.getContent()).hasSize(7); // зависит от фикстуры — можно оставить, но осторожно
        assertThat(page.getContent())
                .anySatisfy(a -> assertThat(a.getFirstName()).isEqualTo("Наринэ"))
                .anySatisfy(a -> assertThat(a.getLastName()).isEqualTo("Абгарян"))
                .anySatisfy(a -> assertThat(a.getFirstName()).isEqualTo("Аристотель"));
    }
}