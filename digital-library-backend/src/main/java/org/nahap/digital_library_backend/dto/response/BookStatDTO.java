package org.nahap.digital_library_backend.dto.response;


public record BookStatDTO(
        Integer bookId,
        String title,
        Double averageRating,
        Long ratingCount,
        Long commentCount,
        Long bookmarkCount
) {}