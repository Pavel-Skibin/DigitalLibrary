package org.nahap.digital_library_backend.dto.response;

import java.time.LocalDateTime;

public record BookmarkResponse(
        Integer id,
        String bookTitle,
        double position,
        String name,
        String notes,
        LocalDateTime createdAt,
        boolean isDeleted
) {}