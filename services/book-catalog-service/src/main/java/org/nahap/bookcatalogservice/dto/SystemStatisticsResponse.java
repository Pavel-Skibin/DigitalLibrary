package org.nahap.bookcatalogservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * System statistics response for frontend
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemStatisticsResponse {
    private Long totalBooks;
    private Long totalUsers;
    private Long totalAuthors;
    private Long totalGenres;
    private Double globalAverageRating;
    private Long totalComments;
    private Long totalRatings;
}
