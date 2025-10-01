package org.nahap.digital_library_backend.dto.response;

import java.util.List;

public record BookDetailResponse(
        Integer id,
        String title,
        String description,
        String filePath,
        List<String> authors,
        List<String> genres,
        Double averageRating,
        Long totalRatings,
        List<CommentResponse> comments
) {}