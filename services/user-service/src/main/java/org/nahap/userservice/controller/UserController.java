package org.nahap.userservice.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.nahap.userservice.dto.request.RegisterRequest;
import org.nahap.userservice.dto.response.UserResponse;
import org.nahap.userservice.security.CustomUserDetails;
import org.nahap.userservice.service.AuthService;
import org.nahap.userservice.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Контроллер для управления пользователями
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    public UserController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    // USER/MOD/ADMIN: GET /api/users/me — мой профиль
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyProfile(@AuthenticationPrincipal CustomUserDetails currentUser) {
        Integer currentUserId = currentUser.getId();
        log.info("Запрос профиля пользователя ID: {}", currentUserId);
        UserResponse response = userService.getUserById(currentUserId);
        return ResponseEntity.ok(response);
    }

    // ПУБЛИЧНАЯ РЕГИСТРАЦИЯ (как в монолите) - POST /api/users
    @PostMapping
    public ResponseEntity<UserResponse> registerUser(@Valid @RequestBody RegisterRequest request) {
        log.info("Регистрация нового пользователя: {}", request.getUsername());
        // Используем AuthService для регистрации
        authService.register(request);
        // Возвращаем пользователя (без токена - клиент должен залогиниться)
        UserResponse user = userService.getUserByUsername(request.getUsername());
        return ResponseEntity.status(201).body(user);
    }

    // ADMIN: GET /api/users — список всех пользователей с поиском и пагинацией
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @RequestParam(required = false) String query,
            Pageable pageable) {
        log.info("Запрос списка пользователей с поиском: '{}', pageable: {}", query, pageable);

        Page<UserResponse> users;
        if (query != null && !query.trim().isEmpty()) {
            users = userService.searchUsers(query.trim(), pageable);
        } else {
            users = userService.getAllUsers(pageable);
        }

        return ResponseEntity.ok(users);
    }

    // ADMIN: GET /api/users/{id} — детали пользователя
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Integer id) {
        log.info("Запрос пользователя ID: {}", id);
        UserResponse user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<UserResponse> getUserByUsername(@PathVariable String username) {
        UserResponse user = userService.getUserByUsername(username);
        return ResponseEntity.ok(user);
    }

    // ADMIN: DELETE /api/users/{id} — мягко удалить (забанить)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> softDeleteUser(@PathVariable Integer id,
                                               @AuthenticationPrincipal CustomUserDetails currentUser) {
        Integer adminId = currentUser.getId();
        log.info("Админ {} удаляет пользователя {}", adminId, id);
        userService.softDeleteUser(id, adminId);
        return ResponseEntity.noContent().build();
    }

    // ADMIN: POST /api/users/{id}/restore — восстановить
    @PostMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> restoreUser(@PathVariable Integer id,
                                            @AuthenticationPrincipal CustomUserDetails currentUser) {
        Integer adminId = currentUser.getId();
        log.info("Админ {} восстанавливает пользователя {}", adminId, id);
        userService.restoreUser(id, adminId);
        return ResponseEntity.ok().build();
    }

    // ADMIN: PUT /api/users/{id}/role — изменить роль
    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> changeUserRole(@PathVariable Integer id,
                                               @RequestBody Integer roleId,
                                               @AuthenticationPrincipal CustomUserDetails currentUser) {
        Integer adminId = currentUser.getId();
        log.info("Админ {} меняет роль пользователя {} на роль ID {}", adminId, id, roleId);
        userService.changeUserRole(id, roleId, adminId);
        return ResponseEntity.ok().build();
    }
}
