package org.nahap.bookcatalogservice.dto.response;


public record BookStatDTO(
        Integer bookId,
        String title,
        Double averageRating,
        Long ratingCount,
        Long commentCount,
        Long bookmarkCount
) {}