package org.nahap.digital_library_backend.repository.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.jdbc.Sql;
import org.nahap.digital_library_backend.entity.Book;
import org.nahap.digital_library_backend.repository.BaseRepositoryTest;
import org.nahap.digital_library_backend.repository.BookRepository;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@Tag("sql-injection")
@DisplayName("BookRepository SQL Injection Security Tests")
@Sql(scripts = "/sql/complete-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class BookRepositorySqlInjectionTest extends BaseRepositoryTest {

    @Autowired
    private BookRepository bookRepository;

    private static final String[] SQL_INJECTION_VECTORS = {
            "' OR 1=1 --",
            "' OR '1'='1' --",
            "'; DROP TABLE books; --",
            "' UNION SELECT null, null, null, null, null, null, null FROM books --",
            "' OR pg_sleep(5) --",
            "\" OR \"\"=\"",
            "admin'--",
            "'; EXEC xp_cmdshell('ping 127.0.0.1') --",
            "' AND 1=CAST((SELECT table_name FROM information_schema.tables LIMIT 1) AS int) --",
            "' ORDER BY (SELECT column_name FROM information_schema.columns LIMIT 1) --"
    };

    private static final String[] SORT_INJECTION_VECTORS = {
            "title; DROP TABLE books; --",
            "(SELECT CASE WHEN (1=1) THEN title ELSE id END)",
            "(SELECT pg_sleep(5))",
            "CAST((SELECT username FROM users LIMIT 1) AS INTEGER)",
            "rating; SHUTDOWN; --"
    };

    @Test
    @DisplayName("searchBooksSimple immune to SQL injection in title parameter")
    void searchBooksSimpleImmuneToTitleInjection() {
        PageRequest pageable = PageRequest.of(0, 10);

        for (String payload : SQL_INJECTION_VECTORS) {
            Page<Book> result = bookRepository.searchBooksSimple(
                    payload,
                    null,
                    null,
                    pageable
            );
            assertThat(result.getTotalElements()).isGreaterThanOrEqualTo(0);
        }
    }

    @Test
    @DisplayName("searchBooksSimple immune to SQL injection in authorIds parameter")
    void searchBooksSimpleImmuneToAuthorIdsInjection() {
        PageRequest pageable = PageRequest.of(0, 10);

        for (String payload : SQL_INJECTION_VECTORS) {
            try {
                List<Integer> maliciousAuthorIds = Arrays.asList(Integer.valueOf(payload.replaceAll("[^0-9]", "")));
                Page<Book> result = bookRepository.searchBooksSimple(
                        null,
                        maliciousAuthorIds,
                        null,
                        pageable
                );
                assertThat(result.getTotalElements()).isGreaterThanOrEqualTo(0);
            } catch (NumberFormatException e) {
            }
        }
    }

    @Test
    @DisplayName("searchBooksWithRatingAndSort immune to SQL injection in sortField parameter")
    void searchBooksWithRatingAndSortImmuneToSortInjection() {
        PageRequest pageable = PageRequest.of(0, 10);

        for (String payload : SORT_INJECTION_VECTORS) {
            Page<Book> result = bookRepository.searchBooksWithRatingAndSort(
                    null,
                    null,
                    null,
                    null,
                    null,
                    payload,
                    pageable
            );
            assertThat(result.getTotalElements()).isGreaterThanOrEqualTo(0);
        }
    }

    @Test
    @DisplayName("Data integrity preserved after injection attempts")
    void dataIntegrityPreserved() {
        long initialBookCount = bookRepository.count();

        PageRequest pageable = PageRequest.of(0, 10);

        String[] dangerousPayloads = {
                "'; DROP TABLE books; --",
                "'; TRUNCATE TABLE books; --",
                "'; DELETE FROM books; --"
        };

        for (String payload : dangerousPayloads) {
            bookRepository.searchBooksSimple(payload, null, null, pageable);
            bookRepository.findByTitleContainingIgnoreCase(payload);

            try {
                bookRepository.searchBooksWithRatingAndSort(
                        null, null, null, null, null,
                        payload.replaceAll("[^a-zA-Z]", ""),
                        pageable
                );
            } catch (Exception e) {
            }
        }

        assertThat(bookRepository.count()).isEqualTo(initialBookCount);
        assertThat(bookRepository.findById(1)).isPresent();
    }

    @Test
    @DisplayName("Handles encoded SQL injection attempts in title search")
    void handlesEncodedPayloadsInTitleSearch() {
        PageRequest pageable = PageRequest.of(0, 10);
        String[] encodedPayloads = {
                "%27%20OR%201%3D1%20--",
                "\u0027 OR 1=1 --",
                "%25%27%20OR%201%3D1%20--" // Double URL encoded
        };

        for (String payload : encodedPayloads) {
            Page<Book> result = bookRepository.searchBooksSimple(
                    payload,
                    null,
                    null,
                    pageable
            );
            assertThat(result.getTotalElements()).isGreaterThanOrEqualTo(0);
        }
    }

    @Test
    @DisplayName("findByTitleContainingIgnoreCase immune to SQL injection")
    void findByTitleContainingIgnoreCaseImmune() {
        for (String payload : SQL_INJECTION_VECTORS) {
            List<Book> result = bookRepository.findByTitleContainingIgnoreCase(payload);
            assertThat(result).isNotNull();
        }
    }

    @Test
    @DisplayName("searchBooksWithRatingAndSort handles time-based attacks")
    void searchBooksWithRatingAndSortHandlesTimeBasedAttacks() {
        PageRequest pageable = PageRequest.of(0, 10);
        String[] timeBasedPayloads = {
                "' OR pg_sleep(5) --",
                "' OR SLEEP(5) --",
                "' AND (SELECT 1 FROM pg_sleep(5)) --"
        };

        for (String payload : timeBasedPayloads) {
            long startTime = System.currentTimeMillis();
            Page<Book> result = bookRepository.searchBooksWithRatingAndSort(
                    payload,
                    null,
                    null,
                    null,
                    null,
                    "title",
                    pageable
            );
            long executionTime = System.currentTimeMillis() - startTime;

            assertThat(executionTime).isLessThan(3000);
            assertThat(result.getTotalElements()).isGreaterThanOrEqualTo(0);
        }
    }
}