package org.nahap.digital_library_backend.dto.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.nahap.digital_library_backend.dto.response.BookmarkResponse;
import org.nahap.digital_library_backend.entity.Bookmark;

@Mapper(componentModel = "spring")
public interface BookmarkMapper {

    @Mapping(target = "bookTitle", source = "book.title")
    @Mapping(target = "isDeleted", expression = "java(bookmark.getDeletedAt() != null)")
    BookmarkResponse toResponse(Bookmark bookmark);
}