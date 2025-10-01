package org.nahap.digital_library_backend.dto.response;

public record UserActivityDTO(
        Integer userId,
        String username,
        Long activityCount,
        String activityType
) {}