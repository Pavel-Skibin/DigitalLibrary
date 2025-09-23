package org.nahap.digital_library_backend.dto.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.nahap.digital_library_backend.dto.request.BookCreateRequest;
import org.nahap.digital_library_backend.dto.response.BookResponse;
import org.nahap.digital_library_backend.entity.Book;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface BookMapper {

    @Mapping(target = "authors", expression = "java(getAuthorNames(book))")
    @Mapping(target = "genres", expression = "java(getGenreNames(book))")
    @Mapping(target = "averageRating", expression = "java(calculateAverageRating(book))")
    BookResponse toResponse(Book book);

    default List<String> getAuthorNames(Book book) {
        if (book.getBookAuthors() == null) return Collections.emptyList();
        return book.getBookAuthors().stream()
                .map(ba -> ba.getAuthor().getFirstName() + " " + ba.getAuthor().getLastName())
                .collect(Collectors.toList());
    }

    default List<String> getGenreNames(Book book) {
        if (book.getBookGenres() == null) return Collections.emptyList();
        return book.getBookGenres().stream()
                .map(bg -> bg.getGenre().getName())
                .collect(Collectors.toList());
    }

    default Double calculateAverageRating(Book book) {
        if (book.getRatings() == null || book.getRatings().isEmpty()) return 0.0;
        return book.getRatings().stream()
                .mapToInt(Rating -> Rating.getValue())
                .average()
                .orElse(0.0);
    }
}