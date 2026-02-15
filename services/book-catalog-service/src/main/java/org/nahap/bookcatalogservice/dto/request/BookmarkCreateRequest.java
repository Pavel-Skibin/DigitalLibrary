package org.nahap.bookcatalogservice.dto.request;

import jakarta.validation.constraints.NotNull;

public record BookmarkCreateRequest(
        @NotNull(message = "ID книги не может быть пустым")
        Integer bookId,

        double position,

        String name,

        String notes
) {}