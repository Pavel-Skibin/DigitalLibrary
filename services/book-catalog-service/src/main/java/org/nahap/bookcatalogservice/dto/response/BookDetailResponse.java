package org.nahap.bookcatalogservice.dto.response;

import java.util.List;

public record BookDetailResponse(
        Integer id,
        String title,
        String description,
        List<String> authors,
        List<String> genres,
        Double averageRating,
        Integer ratingsCount,
        String coverUrl
) {}