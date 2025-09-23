package org.nahap.digital_library_backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserCreateRequest(
        @NotBlank String username,
        @NotBlank String password, // plain text, хешируем в сервисе
        @Email @NotBlank String email,
        @NotNull Integer roleId
) {}