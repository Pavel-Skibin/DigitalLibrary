package org.nahap.digital_library_backend.dto.mapper;

import org.mapstruct.Mapper;
import org.nahap.digital_library_backend.dto.response.GenreResponse;
import org.nahap.digital_library_backend.entity.Genre;

@Mapper(componentModel = "spring")
public interface GenreMapper {
    GenreResponse toResponse(Genre genre);
}