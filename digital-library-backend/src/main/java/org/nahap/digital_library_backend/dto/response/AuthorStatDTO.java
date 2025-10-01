package org.nahap.digital_library_backend.dto.response;

public record AuthorStatDTO(
        Integer authorId,
        String firstName,
        String lastName,
        String fullName,
        Long bookCount,
        Double averageRating
) {}