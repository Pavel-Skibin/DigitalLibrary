package org.nahap.digital_library_backend.dto.response;

import java.time.LocalDateTime;

public record CommentResponse(
        Integer id,
        Integer userId,
        String userName,
        Integer bookId,
        String text,
        LocalDateTime createdAt,
        LocalDateTime deletedAt
) {}