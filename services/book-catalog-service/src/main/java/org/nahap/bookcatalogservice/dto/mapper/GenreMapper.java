package org.nahap.bookcatalogservice.dto.mapper;

import org.mapstruct.Mapper;
import org.nahap.bookcatalogservice.dto.response.GenreResponse;
import org.nahap.bookcatalogservice.entity.Genre;

@Mapper(componentModel = "spring")
public interface GenreMapper {
    GenreResponse toResponse(Genre genre);
}