package org.nahap.bookcatalogservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Active commenter response (matching frontend expectations)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActiveCommenterResponse {
    private Integer userId;
    private String username;
    private Long activityCount;  // Note: activityCount, not commentCount
}
