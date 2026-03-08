package org.nahap.bookcatalogservice.dto.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.nahap.bookcatalogservice.dto.response.AuthorResponse;
import org.nahap.bookcatalogservice.entity.Author;

@Mapper(componentModel = "spring")
public interface AuthorMapper {

    @Mapping(target = "fullName", expression = "java(author.getFirstName() + \" \" + author.getLastName())")
    AuthorResponse toResponse(Author author);
}