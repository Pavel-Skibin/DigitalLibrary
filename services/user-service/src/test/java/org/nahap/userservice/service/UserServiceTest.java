package org.nahap.userservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nahap.common.exception.ResourceNotFoundException;
import org.nahap.userservice.dto.mapper.UserMapper;
import org.nahap.userservice.dto.response.UserResponse;
import org.nahap.userservice.entity.User;
import org.nahap.userservice.entity.UserRole;
import org.nahap.userservice.repository.UserRepository;
import org.nahap.userservice.repository.UserRoleRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UserResponse testUserResponse;

    @BeforeEach
    void setUp() {
        UserRole userRole = new UserRole();
        userRole.setId(1);
        userRole.setName("USER");

        testUser = new User();
        testUser.setId(1);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setRole(userRole);
        testUser.setPasswordHash("hash");

        testUserResponse = new UserResponse(1, "testuser", "test@example.com", "USER");
    }

    @Test
    void getUserById_whenFound_returnsUserResponse() {
        when(userRepository.findActiveById(1)).thenReturn(Optional.of(testUser));
        when(userMapper.toResponse(testUser)).thenReturn(testUserResponse);

        UserResponse result = userService.getUserById(1);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1);
        assertThat(result.getUsername()).isEqualTo("testuser");
    }

    @Test
    void getUserById_whenNotFound_throwsResourceNotFoundException() {
        when(userRepository.findActiveById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getUserByUsername_whenFound_returnsUserResponse() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userMapper.toResponse(testUser)).thenReturn(testUserResponse);

        UserResponse result = userService.getUserByUsername("testuser");

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
    }

    @Test
    void getUserByUsername_whenNotFound_throwsResourceNotFoundException() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserByUsername("unknown"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAllUsers_returnsListOfUserResponses() {
        when(userRepository.findAllActive()).thenReturn(List.of(testUser));
        when(userMapper.toResponse(testUser)).thenReturn(testUserResponse);

        List<UserResponse> result = userService.getAllUsers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUsername()).isEqualTo("testuser");
    }

    @Test
    void softDeleteUser_whenFound_setsDeletedAt() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        userService.softDeleteUser(1, 2);

        assertThat(testUser.getDeletedAt()).isNotNull();
        verify(userRepository).save(testUser);
    }

    @Test
    void softDeleteUser_whenNotFound_throwsResourceNotFoundException() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.softDeleteUser(99, 1))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void softDeleteUser_whenAlreadyDeleted_doesNotUpdateAgain() {
        testUser.setDeletedAt(LocalDateTime.now().minusDays(1));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        LocalDateTime initialDeletedAt = testUser.getDeletedAt();
        userService.softDeleteUser(1, 2);

        assertThat(testUser.getDeletedAt()).isEqualTo(initialDeletedAt);
        verify(userRepository, never()).save(any());
    }

    @Test
    void restoreUser_whenDeleted_clearsDeletedAt() {
        testUser.setDeletedAt(LocalDateTime.now().minusDays(1));
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        userService.restoreUser(1, 2);

        assertThat(testUser.getDeletedAt()).isNull();
        verify(userRepository).save(testUser);
    }
}
