package org.nahap.digital_library_backend.dto.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.nahap.digital_library_backend.dto.response.AuthorResponse;
import org.nahap.digital_library_backend.entity.Author;

@Mapper(componentModel = "spring")
public interface AuthorMapper {

    @Mapping(target = "fullName", expression = "java(author.getFirstName() + \" \" + author.getLastName())")
    AuthorResponse toResponse(Author author);
}