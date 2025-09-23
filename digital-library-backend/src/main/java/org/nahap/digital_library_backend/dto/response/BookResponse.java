package org.nahap.digital_library_backend.dto.response;

import java.util.List;

public record BookResponse(
        Integer id,
        String title,
        String description,
        List<String> authors, // ["Лев Толстой", "Фёдор Достоевский"]
        List<String> genres,  // ["Роман", "Классика"]
        Double averageRating  // 4.5
) {}