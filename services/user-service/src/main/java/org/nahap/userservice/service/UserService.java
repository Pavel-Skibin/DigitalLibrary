package org.nahap.userservice.service;

import lombok.extern.slf4j.Slf4j;
import org.nahap.common.exception.ResourceNotFoundException;
import org.nahap.userservice.dto.response.UserResponse;
import org.nahap.userservice.dto.mapper.UserMapper;
import org.nahap.userservice.entity.User;
import org.nahap.userservice.entity.UserRole;
import org.nahap.userservice.repository.UserRepository;
import org.nahap.userservice.repository.UserRoleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Сервис для управления пользователями
 */
@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, UserRoleRepository userRoleRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.userMapper = userMapper;
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Integer id) {
        User user = userRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return userMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        
        if (user.getDeletedAt() != null) {
            throw new ResourceNotFoundException("User", "username", username);
        }
        
        return userMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAllActive().stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(userMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> searchUsers(String query, Pageable pageable) {
        return userRepository.searchUsers(query, pageable)
                .map(userMapper::toResponse);
    }

    @Transactional
    public void softDeleteUser(Integer id, Integer adminId) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        
        if (user.getDeletedAt() != null) {
            log.warn("Попытка удалить уже удалённого пользователя: {}", id);
            return;
        }
        
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
        log.info("Пользователь {} мягко удалён администратором {}", user.getUsername(), adminId);
    }

    @Transactional
    public void restoreUser(Integer id, Integer adminId) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        
        if (user.getDeletedAt() == null) {
            log.warn("Попытка восстановить неудалённого пользователя: {}", id);
            return;
        }
        
        user.setDeletedAt(null);
        userRepository.save(user);
        log.info("Пользователь {} восстановлен администратором {}", user.getUsername(), adminId);
    }

    @Transactional
    public void changeUserRole(Integer id, Integer newRoleId, Integer adminId) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        
        UserRole newRole = userRoleRepository.findById(newRoleId)
                .orElseThrow(() -> new ResourceNotFoundException("UserRole", "id", newRoleId));
        
        user.setRole(newRole);
        userRepository.save(user);
        log.info("Роль пользователя {} изменена на {} администратором {}", user.getUsername(), newRole.getName(), adminId);
    }

    // Оставляем старый метод для обратной совместимости
    @Transactional
    public void deleteUser(Integer id) {
        softDeleteUser(id, null);
    }
}
