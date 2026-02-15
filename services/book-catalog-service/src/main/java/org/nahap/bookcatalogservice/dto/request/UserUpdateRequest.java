package org.nahap.bookcatalogservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import org.springframework.lang.Nullable;

public record UserUpdateRequest(
        @NotBlank(message = "Логин не может быть пустым")
        String username,

        // Пароль опционален: если null — не меняем, если есть — должен быть не пустым
        @Nullable
        String password,

        @NotBlank(message = "Email не может быть пустым")
        @Email(message = "Некорректный формат email")
        String email
) {}