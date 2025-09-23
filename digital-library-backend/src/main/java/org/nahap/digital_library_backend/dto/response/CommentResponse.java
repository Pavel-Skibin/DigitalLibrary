package org.nahap.digital_library_backend.dto.response;

import java.time.LocalDateTime;

public record CommentResponse(
        Integer id,
        String text,
        String authorUsername,
        LocalDateTime createdAt,
        boolean isDeleted
) {}