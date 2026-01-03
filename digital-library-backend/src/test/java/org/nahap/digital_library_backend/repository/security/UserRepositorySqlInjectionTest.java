package org.nahap.digital_library_backend.repository.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.jdbc.Sql;
import org.nahap.digital_library_backend.repository.BaseRepositoryTest;
import org.nahap.digital_library_backend.repository.UserRepository;
import org.nahap.digital_library_backend.entity.User;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@Tag("sql-injection")
@DisplayName("UserRepository SQL Injection Security Tests")
@Sql(scripts = "/sql/complete-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class UserRepositorySqlInjectionTest extends BaseRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private static final String[] SQL_INJECTION_VECTORS = {
            "' OR 1=1 --",
            "' OR '1'='1' --",
            "'; DROP TABLE users; --",
            "' UNION SELECT username, password FROM users --",
            "' OR IF(1=1, SLEEP(5), 0) --",
            "\" OR \"\"=\"",
            "admin'--",
            "'; EXEC xp_cmdshell('ping 127.0.0.1') --"
    };

    @Test
    @DisplayName("findActiveByUsername immune to SQL injection")
    void findActiveByUsernameImmune() {
        for (String payload : SQL_INJECTION_VECTORS) {
            Optional<User> result = userRepository.findActiveByUsername(payload);
            assertThat(result).isEmpty();
        }
    }

    @Test
    @DisplayName("findActiveByEmail immune to SQL injection")
    void findActiveByEmailImmune() {
        for (String payload : SQL_INJECTION_VECTORS) {
            Optional<User> result = userRepository.findActiveByEmail(payload);
            assertThat(result).isEmpty();
        }
    }

    @Test
    @DisplayName("searchUsers immune to SQL injection")
    void searchUsersImmune() {
        PageRequest pageable = PageRequest.of(0, 10);
        for (String payload : SQL_INJECTION_VECTORS) {
            Page<User> result = userRepository.searchUsers(payload, pageable);
            assertThat(result.getTotalElements()).isEqualTo(0);
        }
    }

    @Test
    @DisplayName("Data integrity preserved after injection attempts")
    void dataIntegrityPreserved() {
        long initialCount = userRepository.count();
        String[] dangerousPayloads = {
                "'; DROP TABLE users; --",
                "'; TRUNCATE TABLE users; --"
        };
        PageRequest pageable = PageRequest.of(0, 10);

        for (String payload : dangerousPayloads) {
            userRepository.findActiveByUsername(payload);
            userRepository.searchUsers(payload, pageable);
        }

        assertThat(userRepository.count()).isEqualTo(initialCount);
        assertThat(userRepository.findByUsername("LiamSmith")).isPresent();
    }

    @Test
    @DisplayName("Handles encoded SQL injection attempts")
    void handlesEncodedPayloads() {
        PageRequest pageable = PageRequest.of(0, 10);
        String[] encodedPayloads = {
                "%27%20OR%201%3D1%20--",
                "\u0027 OR 1=1 --"
        };

        for (String payload : encodedPayloads) {
            assertThat(userRepository.findActiveByUsername(payload)).isEmpty();
            assertThat(userRepository.findActiveByEmail(payload)).isEmpty();
            assertThat(userRepository.searchUsers(payload, pageable).getTotalElements()).isEqualTo(0);
        }
    }
}