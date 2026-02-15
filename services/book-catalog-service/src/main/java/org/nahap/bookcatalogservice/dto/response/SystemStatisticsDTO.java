package org.nahap.bookcatalogservice.dto.response;

public record SystemStatisticsDTO(
        Long totalBooks,
        Long totalAuthors,
        Long totalGenres,
        Long totalUsers,
        Long totalRatings,
        Long totalComments,
        Double globalAverageRating
) {}

