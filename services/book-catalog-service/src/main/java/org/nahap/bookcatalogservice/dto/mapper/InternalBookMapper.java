package org.nahap.bookcatalogservice.dto.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.nahap.bookcatalogservice.api.internal.model.BookResponse;
import org.nahap.bookcatalogservice.entity.Book;
import org.nahap.bookcatalogservice.entity.BookAuthor;
import org.nahap.bookcatalogservice.entity.BookGenre;
import org.nahap.bookcatalogservice.entity.BookTag;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for converting Book entity to OpenAPI generated BookResponse DTO
 */
@Mapper(componentModel = "spring")
public interface InternalBookMapper {

    @Mapping(target = "authors", expression = "java(mapAuthors(book.getBookAuthors()))")
    @Mapping(target = "genres", expression = "java(mapGenres(book.getBookGenres()))")
    @Mapping(target = "tags", expression = "java(mapTags(book.getBookTags()))")
    @Mapping(source = "publicationYear", target = "yearPublished")
    @Mapping(target = "createdAt", expression = "java(toOffsetDateTime(book.getCreatedAt()))")
    @Mapping(target = "isDeleted", constant = "false")
    BookResponse toResponse(Book book);

    default List<String> mapAuthors(List<BookAuthor> bookAuthors) {
        if (bookAuthors == null) {
            return null;
        }
        return bookAuthors.stream()
                .map(ba -> ba.getAuthor().getFirstName() + " " + ba.getAuthor().getLastName())
                .collect(Collectors.toList());
    }

    default List<String> mapGenres(List<BookGenre> bookGenres) {
        if (bookGenres == null) {
            return null;
        }
        return bookGenres.stream()
                .map(bg -> bg.getGenre().getName())
                .collect(Collectors.toList());
    }

    default List<String> mapTags(List<BookTag> bookTags) {
        if (bookTags == null) {
            return null;
        }
        return bookTags.stream()
                .map(bt -> bt.getTag().getName())
                .collect(Collectors.toList());
    }

    default OffsetDateTime toOffsetDateTime(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.atOffset(ZoneOffset.UTC);
    }
}
