package org.nahap.bookcatalogservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Top-rated book response for frontend
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopRatedBookResponse {
    private Integer bookId;
    private String title;
    private Double averageRating;
    private Long ratingCount;
    private Long commentCount;
}
