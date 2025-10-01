package org.nahap.digital_library_backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserCreateRequest(
        @NotBlank(message = "Логин не может быть пустым")
        String username,

        @NotBlank(message = "Пароль не может быть пустым")
        String password,

        @NotBlank(message = "Email не может быть пустым")
        @Email(message = "Некорректный формат email")
        String email,

        @NotNull(message = "ID роли не может быть пустым")
        Integer roleId
) {}