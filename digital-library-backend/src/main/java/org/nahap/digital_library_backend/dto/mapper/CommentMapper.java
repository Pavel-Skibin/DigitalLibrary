package org.nahap.digital_library_backend.dto.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.nahap.digital_library_backend.dto.response.CommentResponse;
import org.nahap.digital_library_backend.entity.Comment;

@Mapper(componentModel = "spring")
public interface CommentMapper {

    @Mapping(target = "userName", source = "user.username")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "bookId", source = "book.id")
    CommentResponse toResponse(Comment comment);
}