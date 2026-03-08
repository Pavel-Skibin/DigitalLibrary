package org.nahap.bookcatalogservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Recent comment response for frontend (matching expected format)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecentCommentResponse {
    private Integer id;
    private Integer bookId;
    private Integer userId;
    private String userName;  // Note: userName, not username
    private String text;      // Note: text, not commentText
    private OffsetDateTime createdAt;
}
