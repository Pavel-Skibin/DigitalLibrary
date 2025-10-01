package org.nahap.digital_library_backend.dto.response;

public record AuthorResponse(
        Integer id,
        String firstName,
        String lastName,
        String fullName
) {}