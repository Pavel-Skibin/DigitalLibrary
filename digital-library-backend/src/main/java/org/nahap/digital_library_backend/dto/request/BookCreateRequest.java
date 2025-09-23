package org.nahap.digital_library_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record BookCreateRequest(
        @NotBlank String title,
        String description,
        @NotBlank String filePath,
        @NotNull List<Integer> authorIds,
        @NotNull List<Integer> genreIds
) {}