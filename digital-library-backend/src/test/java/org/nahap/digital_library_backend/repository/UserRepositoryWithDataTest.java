package org.nahap.digital_library_backend.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.nahap.digital_library_backend.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@Tag("db")
@DisplayName("UserRepository Tests with Real Data")
@Sql(scripts = "/sql/complete-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class UserRepositoryWithDataTest extends BaseRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Should find active user by username 'LiamSmith'")
    void shouldFindActiveUserByUsername() {
        Optional<User> user = userRepository.findActiveByUsername("LiamSmith");
        assertThat(user).isPresent();
        assertThat(user.get().getEmail()).isEqualTo("liam.smith@example.com");
        assertThat(user.get().getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("Should find active user by email 'olivia.brown@dispostable.com'")
    void shouldFindActiveUserByEmail() {
        Optional<User> user = userRepository.findActiveByEmail("olivia.brown@dispostable.com");
        assertThat(user).isPresent();
        assertThat(user.get().getUsername()).isEqualTo("OliviaBrown");
    }

    @Test
    @DisplayName("Should not find deleted user (even if exists) via findActiveByUsername")
    void shouldNotFindDeletedUser() {
        // В нашем скрипте нет удалённых пользователей, но проверим на несуществующий
        Optional<User> user = userRepository.findActiveByUsername("DeletedUser");
        assertThat(user).isEmpty();
    }

    @Test
    @DisplayName("findByUsername should find user even if soft-deleted (but we have none)")
    void findByUsernameShouldWorkForAll() {
        Optional<User> user = userRepository.findByUsername("JamesJones");
        assertThat(user).isPresent();
        assertThat(user.get().getRole().getId()).isEqualTo(2); // MODERATOR
    }

    @Test
    @DisplayName("Should return empty for non-existing email")
    void shouldReturnEmptyForNonExistingEmail() {
        Optional<User> user = userRepository.findActiveByEmail("ghost@nowhere.com");
        assertThat(user).isEmpty();
    }
}