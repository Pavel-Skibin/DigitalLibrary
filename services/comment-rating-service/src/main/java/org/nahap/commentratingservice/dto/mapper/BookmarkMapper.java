package org.nahap.commentratingservice.dto.mapper;

import org.nahap.commentratingservice.entity.Bookmark;
import org.nahap.commentratingservice.dto.response.BookmarkResponse;
import org.springframework.stereotype.Component;

@Component
public class BookmarkMapper {

    public BookmarkResponse toResponse(Bookmark bookmark) {
        // TODO: Get book title from Book Catalog Service via REST
        String bookTitle = "Book#" + bookmark.getBookId();
        
        return new BookmarkResponse(
                bookmark.getId(),
                bookmark.getBookId(),
                bookTitle,
                bookmark.getPosition(),
                bookmark.getName(),
                bookmark.getNotes(),
                bookmark.getCreatedAt(),
                bookmark.getDeletedAt() != null
        );
    }
}
