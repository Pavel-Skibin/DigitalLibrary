package org.nahap.bookcatalogservice.dto.response;

import java.time.LocalDateTime;

public record UserResponse(
        Integer id,
        String username,
        String email,
        String roleName,
        boolean isDeleted,
        LocalDateTime deletedAt
) {}