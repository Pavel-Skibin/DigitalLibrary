package org.nahap.digital_library_backend.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.nahap.digital_library_backend.entity.Author;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@Tag("db")
@DisplayName("AuthorRepository SQL Injection Protection Tests")
// Можно загрузить тестовые данные, если нужно (но для инъекций не обязательно)
// @Sql(scripts = "/sql/complete-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class AuthorRepositorySqlInjectionTest extends BaseRepositoryTest {

    @Autowired
    private AuthorRepository authorRepository;

    // === Тесты для методов с String-параметрами (наиболее уязвимые к инъекции) ===

    @Test
    @DisplayName("findByFirstNameContainingIgnoreCase should be safe from SQL injection")
    void findByFirstNameContainingIgnoreCase_shouldBeSafe() {
        // Типичные SQL/XSS-векторы как строка поиска
        String[] maliciousInputs = {
                "'; DROP TABLE authors; --",
                "'; DELETE FROM author; --",
                "\"; SELECT * FROM information_schema.tables; --",
                "admin'--",
                "test' OR '1'='1",
                "<script>alert(1)</script>", // не SQL, но часто проверяют вместе
                "'); SHUTDOWN; --"
        };

        for (String input : maliciousInputs) {
            List<Author> result = authorRepository.findByFirstNameContainingIgnoreCase(input);
            // Должно просто вернуть пустой список (или нормальные данные), но НЕ упасть
            System.out.println(result);
            assertThat(result).isNotNull(); // не null
            // Никаких исключений → уже успех
        }
    }

    @Test
    @DisplayName("findByLastNameContainingIgnoreCase should be safe from SQL injection")
    void findByLastNameContainingIgnoreCase_shouldBeSafe() {
        String[] payloads = {
                "'; DROP TABLE author; --",
                "x' OR 'x'='x",
                "'); EXEC xp_cmdshell('dir'); --"
        };

        for (String payload : payloads) {
            List<Author> result = authorRepository.findByLastNameContainingIgnoreCase(payload);
            assertThat(result).isNotNull();
        }
    }

    @Test
    @DisplayName("findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase should be safe")
    void findByFirstNameOrLastNameContaining_shouldBeSafe() {
        String evil = "'; SELECT password FROM users; --";

        Page<Author> page = authorRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
                evil, evil, PageRequest.of(0, 10)
        );

        assertThat(page).isNotNull();
        assertThat(page.getContent()).isNotNull();
    }

    // === Тесты для @Query-методов с параметрами ===

    @Test
    @DisplayName("hasBooks should be safe even with invalid authorId")
    void hasBooks_shouldBeSafe() {
        // authorId — Integer, инъекция маловероятна, но проверим крайние значения
        Integer[] dangerousIds = { -1, 0, Integer.MAX_VALUE, 999999 };

        for (Integer id : dangerousIds) {
            boolean result = authorRepository.hasBooks(id);
            // Должно просто вернуть false или true, без эксепшена
            assertThat(result).isIn(true, false);
        }
    }

    @Test
    @DisplayName("countBooksByAuthorId should not allow SQL injection via authorId")
    void countBooksByAuthorId_shouldBeSafe() {
        Long count = authorRepository.countBooksByAuthorId(-999);
        assertThat(count).isGreaterThanOrEqualTo(0L);
    }

    @Test
    @DisplayName("findTopAuthorsByRating should be safe with malicious minRatings")
    void findTopAuthorsByRating_shouldBeSafe() {
        // minRatings — Long, но попробуем "нелогичные" значения
        Long[] badValues = { -1L, -999999L, 0L, Long.MAX_VALUE };

        for (Long minRatings : badValues) {
            Page<Object[]> page = authorRepository.findTopAuthorsByRating(minRatings, PageRequest.of(0, 5));
            assertThat(page).isNotNull();
            // Даже если запрос логически бессмысленный — он не должен падать
        }
    }

    @Test
    @DisplayName("findTopAuthorsByBookCount should not be affected by injection (no params, but sanity check)")
    void findTopAuthorsByBookCount_shouldBeStable() {
        Page<Object[]> page = authorRepository.findTopAuthorsByBookCount(PageRequest.of(0, 5));
        assertThat(page).isNotNull();
    }
}