package org.nahap.commentratingservice.dto.response;

import java.time.LocalDateTime;

public record BookmarkResponse(
        Integer id,
        Integer bookId,
        String bookTitle,
        double position,
        String name,
        String notes,
        LocalDateTime createdAt,
        boolean isDeleted
) {}
