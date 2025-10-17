package org.nahap.digital_library_backend.dto.response;

import java.util.List;

public record BookResponse(
        Integer id,
        String title,
        String description,
        List<String> authors,
        List<String> genres,
        Double averageRating,
        String coverUrl
) {}