package org.nahap.digital_library_backend.dto.response;

public record BookmarkResponse(
        Integer id,
        String bookTitle,
        double position,
        String name,
        String notes,
        boolean isDeleted
) {}