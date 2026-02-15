package org.nahap.bookcatalogservice.dto.response;

public record RatingResponse(
        Integer id,
        Integer bookId,
        Integer value,
        String username
) {}