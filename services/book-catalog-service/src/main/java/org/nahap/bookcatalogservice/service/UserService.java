package org.nahap.bookcatalogservice.service;

import org.nahap.bookcatalogservice.dto.request.UserCreateRequest;
import org.nahap.bookcatalogservice.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    UserResponse registerUser(UserCreateRequest request);

    void softDeleteUser(Integer userId, Integer adminId);

    void restoreUser(Integer userId, Integer adminId);

    void changeUserRole(Integer userId, Integer newRoleId, Integer adminId);

    UserResponse getUserById(Integer userId);

    Page<UserResponse> getAllUsers(Pageable pageable);

    boolean isUsernameOrEmailTaken(String username, String email);

    Page<UserResponse> searchUsers(String query, Pageable pageable); // Добавьте этот метод

}