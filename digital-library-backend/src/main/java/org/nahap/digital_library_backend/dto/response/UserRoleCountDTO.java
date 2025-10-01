package org.nahap.digital_library_backend.dto.response;

public record UserRoleCountDTO(
        String roleName,
        Long userCount
) {}