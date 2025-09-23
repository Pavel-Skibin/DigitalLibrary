package org.nahap.digital_library_backend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;

public record RatingCreateRequest(
        @NotNull Integer bookId,
        @Min(1) @Max(5) Integer value
) {}