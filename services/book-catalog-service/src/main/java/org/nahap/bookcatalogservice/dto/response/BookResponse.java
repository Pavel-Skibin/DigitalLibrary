package org.nahap.bookcatalogservice.dto.response;

import java.util.List;

public record BookResponse(
        Integer id,
        String title,
        String description,
        List<String> authors,
        List<String> genres,
        Double averageRating,
        Integer ratingsCount,
        String coverUrl,
        Integer wordCount,
        String language,
        Integer publicationYear,
        String ageRating,
        String seriesName,
        Integer seriesNumber,
        List<String> tags
) {}