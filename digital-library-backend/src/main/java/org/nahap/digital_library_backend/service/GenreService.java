package org.nahap.digital_library_backend.service;

import org.nahap.digital_library_backend.dto.response.GenreResponse;

import java.util.List;

public interface GenreService {
    GenreResponse createGenre(String name);

    GenreResponse updateGenre(Integer genreId, String name);

    List<GenreResponse> getAllGenres();

    GenreResponse getGenreById(Integer id);

    void deleteGenre(Integer genreId);
}