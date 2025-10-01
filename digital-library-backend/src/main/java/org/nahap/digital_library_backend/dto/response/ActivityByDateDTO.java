package org.nahap.digital_library_backend.dto.response;

public record ActivityByDateDTO(
        String date,        // "2025-10-11"
        Long activityCount
) {}