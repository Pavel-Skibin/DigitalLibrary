package org.nahap.bookcatalogservice.dto.response;

public record BookmarkStatDTO(
        Integer bookId,
        String bookTitle,
        Long bookmarkCount
) {}