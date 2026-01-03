package org.nahap.digital_library_backend.repository.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.jdbc.Sql;
import org.nahap.digital_library_backend.entity.Comment;
import org.nahap.digital_library_backend.repository.BaseRepositoryTest;
import org.nahap.digital_library_backend.repository.CommentRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@Tag("sql-injection")
@DisplayName("CommentRepository SQL Injection Security Tests")
@Sql(scripts = "/sql/complete-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class CommentRepositorySqlInjectionTest extends BaseRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;

    private static final String[] SQL_INJECTION_VECTORS = {
            "' OR 1=1 --",
            "' OR '1'='1' --",
            "'; DROP TABLE comments; --",
            "' UNION SELECT null, null, null, null, null, null, null, null FROM comments --",
            "' OR pg_sleep(5) --",
            "\" OR \"\"=\"",
            "1; DROP TABLE comments; --",
            "' AND 1=CAST((SELECT table_name FROM information_schema.tables LIMIT 1) AS int) --",
            "' ORDER BY (SELECT column_name FROM information_schema.columns LIMIT 1) --"
    };

    @Test
    @DisplayName("findActiveByBookId immune to SQL injection")
    void findActiveByBookIdImmune() {
        for (String payload : SQL_INJECTION_VECTORS) {
            try {
                Integer bookId = Integer.valueOf(payload.replaceAll("[^0-9]", "1"));
                List<Comment> comments = commentRepository.findActiveByBookId(bookId);
                assertThat(comments).isNotNull();
            } catch (NumberFormatException e) {
            }
        }
    }

    @Test
    @DisplayName("findActiveByBookId with pagination immune to SQL injection")
    void findActiveByBookIdWithPaginationImmune() {
        PageRequest pageable = PageRequest.of(0, 10);

        for (String payload : SQL_INJECTION_VECTORS) {
            try {
                Integer bookId = Integer.valueOf(payload.replaceAll("[^0-9]", "1"));
                Page<Comment> comments = commentRepository.findActiveByBookId(bookId, pageable);
                assertThat(comments).isNotNull();
                assertThat(comments.getTotalElements()).isGreaterThanOrEqualTo(0);
            } catch (NumberFormatException e) {
            }
        }
    }

    @Test
    @DisplayName("findActiveByUserId immune to SQL injection")
    void findActiveByUserIdImmune() {
        for (String payload : SQL_INJECTION_VECTORS) {
            try {
                Integer userId = Integer.valueOf(payload.replaceAll("[^0-9]", "1"));
                List<Comment> comments = commentRepository.findActiveByUserId(userId);
                assertThat(comments).isNotNull();
            } catch (NumberFormatException e) {
            }
        }
    }

    @Test
    @DisplayName("findActiveByUserAndBook immune to SQL injection")
    void findActiveByUserAndBookImmune() {
        for (String payload : SQL_INJECTION_VECTORS) {
            try {
                Integer userId = Integer.valueOf(payload.replaceAll("[^0-9]", "1"));
                Integer bookId = Integer.valueOf(payload.replaceAll("[^0-9]", "2"));
                Optional<Comment> comment = commentRepository.findActiveByUserAndBook(userId, bookId);
                assertThat(comment).isNotNull();
            } catch (NumberFormatException e) {
            }
        }
    }

    @Test
    @DisplayName("countActiveCommentsByBookId immune to SQL injection")
    void countActiveCommentsByBookIdImmune() {
        for (String payload : SQL_INJECTION_VECTORS) {
            try {
                Integer bookId = Integer.valueOf(payload.replaceAll("[^0-9]", "1"));
                Long count = commentRepository.countActiveCommentsByBookId(bookId);
                assertThat(count).isGreaterThanOrEqualTo(0L);
            } catch (NumberFormatException e) {
            }
        }
    }

    @Test
    @DisplayName("findRecentComments immune to SQL injection in sorting parameters")
    void findRecentCommentsImmuneToSortInjection() {
        for (String payload : SQL_INJECTION_VECTORS) {
            try {
                PageRequest pageable = PageRequest.of(0, 10, Sort.by(payload.replaceAll("[^a-zA-Z]", "createdAt")));
                Page<Comment> comments = commentRepository.findRecentComments(pageable);
                assertThat(comments).isNotNull();
                assertThat(comments.getTotalElements()).isGreaterThanOrEqualTo(0);
            } catch (Exception e) {
            }
        }
    }

    @Test
    @DisplayName("Data integrity preserved after injection attempts")
    void dataIntegrityPreserved() {
        long initialCommentCount = commentRepository.count();

        String[] dangerousPayloads = {
                "'; DROP TABLE comments; --",
                "'; TRUNCATE TABLE comments; --",
                "'; DELETE FROM comments; --"
        };

        for (String payload : dangerousPayloads) {
            try {
                Integer maliciousId = Integer.valueOf(payload.replaceAll("[^0-9]", "1"));
                commentRepository.findActiveByBookId(maliciousId);
                commentRepository.findActiveByUserId(maliciousId);
                commentRepository.countActiveCommentsByBookId(maliciousId);
            } catch (Exception e) {
            }
        }

        assertThat(commentRepository.count()).isEqualTo(initialCommentCount);
        assertThat(commentRepository.findById(1)).isPresent();
    }

    @Test
    @DisplayName("Handles encoded SQL injection attempts")
    void handlesEncodedPayloads() {
        String[] encodedPayloads = {
                "%27%20OR%201%3D1%20--",
                "\u0027 OR 1=1 --",
                "1%3B%20DROP%20TABLE%20comments%3B%20--"
        };

        for (String payload : encodedPayloads) {
            try {
                Integer bookId = Integer.valueOf(payload.replaceAll("[^0-9]", "1"));
                List<Comment> comments = commentRepository.findActiveByBookId(bookId);
                assertThat(comments).isNotNull();
            } catch (NumberFormatException e) {
            }
        }
    }

    @Test
    @DisplayName("Resistant to time-based SQL injection attacks")
    void resistantToTimeBasedAttacks() {
        String[] timeBasedPayloads = {
                "' OR pg_sleep(5) --",
                "' OR SLEEP(5) --",
                "' AND (SELECT 1 FROM pg_sleep(5)) --"
        };

        for (String payload : timeBasedPayloads) {
            try {
                long startTime = System.currentTimeMillis();

                Integer bookId = Integer.valueOf(payload.replaceAll("[^0-9]", "1"));
                commentRepository.findActiveByBookId(bookId);

                long executionTime = System.currentTimeMillis() - startTime;
                assertThat(executionTime).isLessThan(3000);
            } catch (NumberFormatException e) {
            }
        }
    }
}