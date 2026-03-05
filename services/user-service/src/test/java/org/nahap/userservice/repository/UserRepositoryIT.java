package org.nahap.userservice.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nahap.userservice.entity.User;
import org.nahap.userservice.entity.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryIT {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    private UserRole userRole;
    private User alice;
    private User bob;
    private User deletedUser;

    @BeforeEach
    void setUp() {
        userRole = userRoleRepository.save(new UserRole(null, "USER", null));

        alice = userRepository.save(new User(null, "alice", "hash_alice", userRole, "alice@test.com", null));
        bob = userRepository.save(new User(null, "bob", "hash_bob", userRole, "bob@test.com", null));
        deletedUser = userRepository.save(
                new User(null, "deleted_user", "hash_del", userRole, "deleted@test.com", LocalDateTime.now())
        );
    }

    @Test
    void findByUsername_existingUser_returnsUser() {
        Optional<User> result = userRepository.findByUsername("alice");
        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("alice@test.com");
    }

    @Test
    void findByUsername_nonExisting_returnsEmpty() {
        Optional<User> result = userRepository.findByUsername("nosuchuser");
        assertThat(result).isEmpty();
    }

    @Test
    void findByEmail_existingEmail_returnsUser() {
        Optional<User> result = userRepository.findByEmail("bob@test.com");
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("bob");
    }

    @Test
    void findActiveByUsername_deletedUser_returnsEmpty() {
        Optional<User> result = userRepository.findActiveByUsername("deleted_user");
        assertThat(result).isEmpty();
    }

    @Test
    void findActiveByUsername_activeUser_returnsUser() {
        Optional<User> result = userRepository.findActiveByUsername("alice");
        assertThat(result).isPresent();
    }

    @Test
    void findActiveById_deletedUser_returnsEmpty() {
        Optional<User> result = userRepository.findActiveById(deletedUser.getId());
        assertThat(result).isEmpty();
    }

    @Test
    void findActiveById_activeUser_returnsUser() {
        Optional<User> result = userRepository.findActiveById(alice.getId());
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("alice");
    }

    @Test
    void findAllActive_excludesDeleted() {
        List<User> active = userRepository.findAllActive();
        assertThat(active).hasSize(2);
        assertThat(active).extracting(User::getUsername)
                .containsExactlyInAnyOrder("alice", "bob");
    }

    @Test
    void countActiveUsers_excludesDeleted() {
        Long count = userRepository.countActiveUsers();
        assertThat(count).isEqualTo(2L);
    }

    @Test
    void searchUsers_byUsername_returnsMatch() {
        Page<User> result = userRepository.searchUsers("alic", PageRequest.of(0, 10));
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUsername()).isEqualTo("alice");
    }

    @Test
    void searchUsers_noQuery_returnsAll() {
        Page<User> result = userRepository.searchUsers(null, PageRequest.of(0, 10));
        // null query returns all users (active + deleted)
        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    @Test
    void countUsersByRole_returnsGroupedCounts() {
        List<Object[]> counts = userRepository.countUsersByRole();
        assertThat(counts).hasSize(1);
        String roleName = (String) counts.get(0)[0];
        Long count = (Long) counts.get(0)[1];
        assertThat(roleName).isEqualTo("USER");
        assertThat(count).isEqualTo(2L); // alice + bob (deleted excluded)
    }
}
