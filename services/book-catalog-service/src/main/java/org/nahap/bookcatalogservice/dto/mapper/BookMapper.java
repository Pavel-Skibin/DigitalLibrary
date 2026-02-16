package org.nahap.bookcatalogservice.dto.mapper;

import org.mapstruct.*;
import org.nahap.bookcatalogservice.dto.response.BookDetailResponse;
import org.nahap.bookcatalogservice.dto.response.BookResponse;
import org.nahap.bookcatalogservice.entity.*;
import org.nahap.bookcatalogservice.repository.BookAuthorRepository;
import org.nahap.bookcatalogservice.repository.BookGenreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Маппер для преобразования сущности Book в DTO
 */
@Mapper(componentModel = "spring")
public abstract class BookMapper {

    @Autowired
    protected BookAuthorRepository bookAuthorRepository;

    @Autowired
    protected BookGenreRepository bookGenreRepository;

    /**
     * Преобразует Book в BookResponse БЕЗ обложки (для StatisticsService)
     */
    public BookResponse toResponse(Book book) {
        if (book == null) {
            return null;
        }

        List<String> authors = getAuthorNames(book);
        List<String> genres = getGenreNames(book);

        return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getDescription(),
                authors,
                genres,
                book.getAverageRating() != null ? book.getAverageRating().doubleValue() : 0.0,
                book.getRatingsCount() != null ? book.getRatingsCount() : 0,
                null // coverUrl не нужен для статистики
        );
    }

    /**
     * Преобразует Book в BookResponse С обложкой (для BookService)
     */
    public BookResponse toResponseWithCover(Book book) {
        if (book == null) {
            return null;
        }

        List<Integer> bookIds = Collections.singletonList(book.getId());
        Map<Integer, List<String>> authorsByBookId = loadAuthorNamesByBookId(bookIds);
        Map<Integer, List<String>> genresByBookId = loadGenreNamesByBookId(bookIds);

        String coverUrl = book.getCoverImagePath() != null
                ? "/api/books/" + book.getId() + "/cover"
                : null;

        return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getDescription(),
                authorsByBookId.getOrDefault(book.getId(), Collections.emptyList()),
                genresByBookId.getOrDefault(book.getId(), Collections.emptyList()),
                book.getAverageRating() != null ? book.getAverageRating().doubleValue() : 0.0,
                book.getRatingsCount() != null ? book.getRatingsCount() : 0,
                coverUrl
        );
    }

    /**
     * Преобразует Page<Book> в Page<BookResponse> С обложками
     */
    public Page<BookResponse> toResponsePageWithCovers(Page<Book> bookPage) {
        if (bookPage.isEmpty()) {
            return Page.empty(bookPage.getPageable());
        }

        List<Integer> bookIds = bookPage.getContent().stream()
                .map(Book::getId)
                .toList();

        Map<Integer, List<String>> authorsByBookId = loadAuthorNamesByBookId(bookIds);
        Map<Integer, List<String>> genresByBookId = loadGenreNamesByBookId(bookIds);

        List<BookResponse> responses = bookPage.getContent().stream()
                .map(book -> {
                    String coverUrl = book.getCoverImagePath() != null
                            ? "/api/books/" + book.getId() + "/cover"
                            : null;

                    return new BookResponse(
                            book.getId(),
                            book.getTitle(),
                            book.getDescription(),
                            authorsByBookId.getOrDefault(book.getId(), Collections.emptyList()),
                            genresByBookId.getOrDefault(book.getId(), Collections.emptyList()),
                            book.getAverageRating() != null ? book.getAverageRating().doubleValue() : 0.0,
                            book.getRatingsCount() != null ? book.getRatingsCount() : 0,
                            coverUrl
                    );
                })
                .collect(Collectors.toList());

        return new PageImpl<>(responses, bookPage.getPageable(), bookPage.getTotalElements());
    }

    /**
     * Преобразует List<Book> в List<BookResponse> С обложками
     */
    public List<BookResponse> toResponseListWithCovers(List<Book> books) {
        if (books == null || books.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> bookIds = books.stream().map(Book::getId).toList();

        Map<Integer, List<String>> authorsByBookId = loadAuthorNamesByBookId(bookIds);
        Map<Integer, List<String>> genresByBookId = loadGenreNamesByBookId(bookIds);

        return books.stream()
                .map(book -> {
                    String coverUrl = book.getCoverImagePath() != null
                            ? "/api/books/" + book.getId() + "/cover"
                            : null;

                    return new BookResponse(
                            book.getId(),
                            book.getTitle(),
                            book.getDescription(),
                            authorsByBookId.getOrDefault(book.getId(), Collections.emptyList()),
                            genresByBookId.getOrDefault(book.getId(), Collections.emptyList()),
                            book.getAverageRating() != null ? book.getAverageRating().doubleValue() : null,
                            book.getRatingsCount(),
                            coverUrl
                    );
                })
                .toList();
    }

    /**
     * Преобразует Book в BookDetailResponse С обложкой
     */
    public BookDetailResponse toDetailResponseWithCover(Book book) {
        if (book == null) {
            return null;
        }

        List<String> authors = getAuthorNames(book);
        List<String> genres = getGenreNames(book);

        String coverUrl = book.getCoverImagePath() != null
                ? "/api/books/" + book.getId() + "/cover"
                : null;

        return new BookDetailResponse(
                book.getId(),
                book.getTitle(),
                book.getDescription(),
                authors,
                genres,
                book.getAverageRating() != null ? book.getAverageRating().doubleValue() : 0.0,
                book.getRatingsCount() != null ? book.getRatingsCount() : 0,
                coverUrl
        );
    }

    protected List<String> getAuthorNames(Book book) {
        if (book.getBookAuthors() == null) return Collections.emptyList();
        return book.getBookAuthors().stream()
                .map(ba -> ba.getAuthor().getFirstName() + " " + ba.getAuthor().getLastName())
                .collect(Collectors.toList());
    }

    protected List<String> getGenreNames(Book book) {
        if (book.getBookGenres() == null) return Collections.emptyList();
        return book.getBookGenres().stream()
                .map(bg -> bg.getGenre().getName())
                .collect(Collectors.toList());
    }

    protected Map<Integer, List<String>> loadAuthorNamesByBookId(List<Integer> bookIds) {
        List<BookAuthor> bookAuthors = bookAuthorRepository.findByBookIdIn(bookIds);
        return bookAuthors.stream()
                .collect(Collectors.groupingBy(
                        ba -> ba.getBook().getId(),
                        Collectors.mapping(
                                ba -> ba.getAuthor().getFirstName() + " " + ba.getAuthor().getLastName(),
                                Collectors.toList()
                        )
                ));
    }

    protected Map<Integer, List<String>> loadGenreNamesByBookId(List<Integer> bookIds) {
        List<BookGenre> bookGenres = bookGenreRepository.findByBookIdIn(bookIds);
        return bookGenres.stream()
                .collect(Collectors.groupingBy(
                        bg -> bg.getBook().getId(),
                        Collectors.mapping(
                                bg -> bg.getGenre().getName(),
                                Collectors.toList()
                        )
                ));
    }
}