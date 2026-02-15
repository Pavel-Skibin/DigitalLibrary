package org.nahap.bookcatalogservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record BookUpdateRequest(
        @NotBlank(message = "Название книги не может быть пустым")
        String title,

        String description,

        String filePath,

        List<@NotNull Integer> authorIds,

        List<@NotNull Integer> genreIds
) {}