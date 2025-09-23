package org.nahap.digital_library_backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserUpdateRequest(
        @NotBlank String username,
        String password,
        @Email @NotBlank String email
) {}