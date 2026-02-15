package org.nahap.bookcatalogservice.dto.response;

public record RatingDistributionDTO(
        Integer ratingValue,
        Long count
) {}