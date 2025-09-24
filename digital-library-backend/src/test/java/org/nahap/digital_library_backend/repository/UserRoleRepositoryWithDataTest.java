package org.nahap.digital_library_backend.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.nahap.digital_library_backend.entity.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@Tag("db")
@DisplayName("UserRoleRepository Tests with Real Data")
@Sql(scripts = "/sql/complete-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class UserRoleRepositoryWithDataTest extends BaseRepositoryTest {

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Test
    @DisplayName("Should find ADMIN role by exact name 'ADMIN'")
    void shouldFindAdminRoleByName() {
        // when
        Optional<UserRole> role = userRoleRepository.findByName("ADMIN");

        // then
        assertThat(role).isPresent();
        assertThat(role.get().getId()).isEqualTo(1);
        assertThat(role.get().getName()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("Should find MODERATOR role by exact name 'MODERATOR'")
    void shouldFindModeratorRoleByName() {
        // when
        Optional<UserRole> role = userRoleRepository.findByName("MODERATOR");

        // then
        assertThat(role).isPresent();
        assertThat(role.get().getId()).isEqualTo(2);
        assertThat(role.get().getName()).isEqualTo("MODERATOR");
    }

    @Test
    @DisplayName("Should find USER role by exact name 'USER'")
    void shouldFindUserRoleByName() {
        // when
        Optional<UserRole> role = userRoleRepository.findByName("USER");

        // then
        assertThat(role).isPresent();
        assertThat(role.get().getId()).isEqualTo(3);
        assertThat(role.get().getName()).isEqualTo("USER");
    }

    @Test
    @DisplayName("Should return empty Optional when role name does not exist")
    void shouldReturnEmptyWhenRoleNotFound() {
        // when
        Optional<UserRole> role = userRoleRepository.findByName("GUEST");

        // then
        assertThat(role).isEmpty();
    }

    @Test
    @DisplayName("Should be case-sensitive: 'admin' should not match 'ADMIN'")
    void shouldBeCaseSensitive() {
        // when
        Optional<UserRole> role = userRoleRepository.findByName("admin");

        // then
        assertThat(role).isEmpty();
    }
}