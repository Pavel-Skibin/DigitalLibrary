package org.nahap.bookcatalogservice.dto.response;

public record AuthResponse(
        String token,
        String username
) {}