package org.nahap.bookcatalogservice.dto.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.nahap.bookcatalogservice.dto.response.RatingResponse;
import org.nahap.bookcatalogservice.entity.Rating;

@Mapper(componentModel = "spring")
public interface RatingMapper {

    @Mapping(target = "bookId", source = "book.id")
    @Mapping(target = "username", source = "user.username")
    RatingResponse toResponse(Rating rating);
}