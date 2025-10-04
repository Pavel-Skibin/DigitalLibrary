package org.nahap.digital_library_backend.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nahap.digital_library_backend.dto.request.UserCreateRequest;
import org.nahap.digital_library_backend.dto.response.UserResponse;
import org.nahap.digital_library_backend.entity.User;
import org.nahap.digital_library_backend.entity.UserRole;
import org.nahap.digital_library_backend.dto.mapper.UserMapper;
import org.nahap.digital_library_backend.exception.RoleNotFoundException;
import org.nahap.digital_library_backend.exception.UserAlreadyExistsException;
import org.nahap.digital_library_backend.exception.UserNotFoundException;
import org.nahap.digital_library_backend.repository.UserRepository;
import org.nahap.digital_library_backend.repository.UserRoleRepository;
import org.nahap.digital_library_backend.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponse registerUser(UserCreateRequest request) {
        if (userRepository.findActiveByUsername(request.username()).isPresent()) {
            throw new UserAlreadyExistsException("Пользователь с таким логином уже существует");
        }
        if (userRepository.findActiveByEmail(request.email()).isPresent()) {
            throw new UserAlreadyExistsException("Пользователь с таким email уже существует");
        }

        UserRole role = userRoleRepository.findById(request.roleId())
                .orElseThrow(() -> new RoleNotFoundException("Роль с ID " + request.roleId() + " не найдена"));

        User user = userMapper.toEntity(request);
        user.setRole(role);
        user.setPasswordHash(passwordEncoder.encode(request.password()));

        User savedUser = userRepository.save(user);
        log.info("Зарегистрирован новый пользователь: {}", savedUser.getUsername());

        return userMapper.toResponse(savedUser);
    }

    @Override
    public Page<UserResponse> searchUsers(String query, Pageable pageable) {
        return userRepository.searchUsers(query, pageable)
                .map(userMapper::toResponse);
    }

    @Override
    public void softDeleteUser(Integer userId, Integer adminId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь с ID " + userId + " не найден"));

        if (user.getDeletedAt() != null) {
            log.warn("Попытка удалить уже удалённого пользователя: {}", userId);
            return;
        }

        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
        log.info("Пользователь {} мягко удалён администратором {}", user.getUsername(), adminId);
    }

    @Override
    public void restoreUser(Integer userId, Integer adminId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь с ID " + userId + " не найден"));

        if (user.getDeletedAt() == null) {
            log.warn("Попытка восстановить неудалённого пользователя: {}", userId);
            return;
        }

        user.setDeletedAt(null);
        userRepository.save(user);
        log.info("Пользователь {} восстановлен администратором {}", user.getUsername(), adminId);
    }

    @Override
    public void changeUserRole(Integer userId, Integer newRoleId, Integer adminId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь с ID " + userId + " не найден"));

        UserRole newRole = userRoleRepository.findById(newRoleId)
                .orElseThrow(() -> new RoleNotFoundException("Роль с ID " + newRoleId + " не найдена"));

        user.setRole(newRole);
        userRepository.save(user);
        log.info("Роль пользователя {} изменена на {} администратором {}", user.getUsername(), newRole.getName(), adminId);
    }

    @Override
    public UserResponse getUserById(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь с ID " + userId + " не найден"));
        return userMapper.toResponse(user);
    }

    @Override
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(userMapper::toResponse);
    }

    @Override
    public boolean isUsernameOrEmailTaken(String username, String email) {
        boolean usernameTaken = userRepository.findActiveByUsername(username).isPresent();
        boolean emailTaken = userRepository.findActiveByEmail(email).isPresent();
        return usernameTaken || emailTaken;
    }
}