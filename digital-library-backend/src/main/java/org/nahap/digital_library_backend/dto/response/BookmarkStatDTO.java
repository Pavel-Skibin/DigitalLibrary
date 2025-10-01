package org.nahap.digital_library_backend.dto.response;

public record BookmarkStatDTO(
        Integer bookId,
        String bookTitle,
        Long bookmarkCount
) {}