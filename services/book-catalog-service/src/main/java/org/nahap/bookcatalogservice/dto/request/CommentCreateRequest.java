package org.nahap.bookcatalogservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CommentCreateRequest(
        @NotNull Integer bookId,
        @NotBlank String text
) {}