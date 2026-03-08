package org.nahap.bookcatalogservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Top author by book count response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopAuthorResponse {
    private Integer authorId;
    private String fullName;
    private Long bookCount;
}
