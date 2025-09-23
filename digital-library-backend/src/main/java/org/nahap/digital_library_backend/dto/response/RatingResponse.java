package org.nahap.digital_library_backend.dto.response;

public record RatingResponse(
        Integer id,
        Integer value,
        String username // кто поставил оценку
) {}