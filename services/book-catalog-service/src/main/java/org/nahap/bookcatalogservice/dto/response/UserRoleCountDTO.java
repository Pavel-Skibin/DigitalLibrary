package org.nahap.bookcatalogservice.dto.response;

public record UserRoleCountDTO(
        String roleName,
        Long userCount
) {}