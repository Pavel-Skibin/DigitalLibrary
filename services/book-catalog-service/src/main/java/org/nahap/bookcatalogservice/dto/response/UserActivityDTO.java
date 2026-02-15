package org.nahap.bookcatalogservice.dto.response;

public record UserActivityDTO(
        Integer userId,
        String username,
        Long activityCount,
        String activityType
) {}