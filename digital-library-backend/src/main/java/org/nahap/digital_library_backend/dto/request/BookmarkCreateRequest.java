package org.nahap.digital_library_backend.dto.request;

import jakarta.validation.constraints.NotNull;

public record BookmarkCreateRequest(
        @NotNull Integer bookId,
        double position,
        String name,
        String notes
) {}