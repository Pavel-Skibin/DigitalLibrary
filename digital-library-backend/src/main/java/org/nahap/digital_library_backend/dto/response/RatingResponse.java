package org.nahap.digital_library_backend.dto.response;

public record RatingResponse(
        Integer id,
        Integer bookId,
        Integer value,
        String username
) {}