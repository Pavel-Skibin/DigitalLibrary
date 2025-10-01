// src/main/java/org/nahap/digital_library_backend/dto/response/GenreStatDTO.java
package org.nahap.digital_library_backend.dto.response;

public record GenreStatDTO(
        Integer genreId,
        String name,
        Long bookCount,      // Количество книг в жанре
        Double averageRating // Средний рейтинг книг жанра
) {}