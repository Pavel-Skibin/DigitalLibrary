package org.nahap.digital_library_backend.dto.response;

public record RatingDistributionDTO(
        Integer ratingValue,
        Long count
) {}