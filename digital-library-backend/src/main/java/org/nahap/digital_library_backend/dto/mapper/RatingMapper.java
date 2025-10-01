package org.nahap.digital_library_backend.dto.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.nahap.digital_library_backend.dto.response.RatingResponse;
import org.nahap.digital_library_backend.entity.Rating;

@Mapper(componentModel = "spring")
public interface RatingMapper {

    @Mapping(target = "bookId", source = "book.id")
    @Mapping(target = "username", source = "user.username")
    RatingResponse toResponse(Rating rating);
}