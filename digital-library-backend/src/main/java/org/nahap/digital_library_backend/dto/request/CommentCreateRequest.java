package org.nahap.digital_library_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CommentCreateRequest(
        @NotNull Integer bookId,
        @NotBlank String text
) {}