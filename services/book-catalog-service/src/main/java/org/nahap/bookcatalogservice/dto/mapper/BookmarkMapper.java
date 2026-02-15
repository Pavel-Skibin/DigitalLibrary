package org.nahap.bookcatalogservice.dto.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.nahap.bookcatalogservice.dto.response.BookmarkResponse;
import org.nahap.bookcatalogservice.entity.Bookmark;

@Mapper(componentModel = "spring")
public interface BookmarkMapper {

    @Mapping(target = "bookTitle", source = "book.title")
    @Mapping(target = "isDeleted", expression = "java(bookmark.getDeletedAt() != null)")
    BookmarkResponse toResponse(Bookmark bookmark);
}