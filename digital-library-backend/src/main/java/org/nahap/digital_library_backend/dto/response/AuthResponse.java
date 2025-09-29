package org.nahap.digital_library_backend.dto.response;

public record AuthResponse(
        String token,
        String username
) {}