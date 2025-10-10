package org.nahap.digital_library_backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nahap.digital_library_backend.dto.request.UserCreateRequest;
import org.nahap.digital_library_backend.dto.response.UserResponse;
import org.nahap.digital_library_backend.security.CustomUserDetails;
import org.nahap.digital_library_backend.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    //  USER/MOD/ADMIN: GET /api/users/me — мой профиль
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyProfile(@AuthenticationPrincipal CustomUserDetails currentUser) {
        Integer currentUserId = currentUser.getId();
        log.info("Запрос профиля пользователя ID: {}", currentUserId);
        UserResponse response = userService.getUserById(currentUserId);
        return ResponseEntity.ok(response);
    }

    //  ADMIN: POST /api/users — создать пользователя
    @PostMapping
    public ResponseEntity<UserResponse> registerUser(@Valid @RequestBody UserCreateRequest request) {
        log.info("Регистрация нового пользователя: {}", request.username());
        UserResponse response = userService.registerUser(request);
        return ResponseEntity.status(201).body(response);
    }

    //  ADMIN: GET /api/users — список всех пользователей с поиском и пагинацией
    @GetMapping
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

    //  ADMIN: GET /api/users/{id} — детали пользователя
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Integer id) {
        log.info("Запрос пользователя ID: {}", id);
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(response);
    }

    //  ADMIN: DELETE /api/users/{id} — мягко удалить (забанить)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDeleteUser(@PathVariable Integer id,
                                               @AuthenticationPrincipal CustomUserDetails currentUser) {
        Integer adminId = currentUser.getId();
        log.info("Админ {} удаляет пользователя {}", adminId, id);
        userService.softDeleteUser(id, adminId);
        return ResponseEntity.noContent().build();
    }

    //  ADMIN: POST /api/users/{id}/restore — восстановить
    @PostMapping("/{id}/restore")
    public ResponseEntity<Void> restoreUser(@PathVariable Integer id,
                                            @AuthenticationPrincipal CustomUserDetails currentUser) {
        Integer adminId = currentUser.getId();
        log.info("Админ {} восстанавливает пользователя {}", adminId, id);
        userService.restoreUser(id, adminId);
        return ResponseEntity.ok().build();
    }

    //  ADMIN: PUT /api/users/{id}/role — изменить роль
    @PutMapping("/{id}/role")
    public ResponseEntity<Void> changeUserRole(@PathVariable Integer id,
                                               @RequestBody Integer roleId,
                                               @AuthenticationPrincipal CustomUserDetails currentUser) {
        Integer adminId = currentUser.getId();
        log.info("Админ {} меняет роль пользователя {} на роль ID {}", adminId, id, roleId);
        userService.changeUserRole(id, roleId, adminId);
        return ResponseEntity.ok().build();
    }
}