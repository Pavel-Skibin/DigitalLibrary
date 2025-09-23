package org.nahap.digital_library_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record BookUpdateRequest(
        @NotBlank String title,
        String description,
        String filePath,
        List<Integer> authorIds,
        List<Integer> genreIds
) {}