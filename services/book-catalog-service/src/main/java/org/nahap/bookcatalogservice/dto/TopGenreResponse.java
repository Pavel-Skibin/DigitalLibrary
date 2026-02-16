package org.nahap.bookcatalogservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Top genre by book count response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopGenreResponse {
    private Integer genreId;
    private String name;
    private Long bookCount;
}
