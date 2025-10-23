package org.nahap.digital_library_backend.dto.mapper;

import org.mapstruct.*;
import org.nahap.digital_library_backend.dto.response.BookDetailResponse;
import org.nahap.digital_library_backend.dto.response.BookResponse;
import org.nahap.digital_library_backend.dto.response.CommentResponse;
import org.nahap.digital_library_backend.entity.*;
import org.nahap.digital_library_backend.repository.BookAuthorRepository;
import org.nahap.digital_library_backend.repository.BookGenreRepository;
import org.nahap.digital_library_backend.repository.RatingRepository;
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

    @Autowired
    protected RatingRepository ratingRepository;

    /**
     * Преобразует Book в BookResponse БЕЗ обложки (для StatisticsService)
     */
    public BookResponse toResponse(Book book) {
        if (book == null) {
            return null;
        }

        List<String> authors = getAuthorNames(book);
        List<String> genres = getGenreNames(book);
        Double avgRating = calculateAverageRating(book);

        return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getDescription(),
                authors,
                genres,
                avgRating > 0 ? avgRating : null,
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
        Map<Integer, Double> ratingsByBookId = loadAverageRatingsByBookId(bookIds);

        String coverUrl = book.getCoverImagePath() != null
                ? "/api/books/" + book.getId() + "/cover"
                : null;

        return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getDescription(),
                authorsByBookId.getOrDefault(book.getId(), Collections.emptyList()),
                genresByBookId.getOrDefault(book.getId(), Collections.emptyList()),
                ratingsByBookId.getOrDefault(book.getId(), 0.0),
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
        Map<Integer, Double> ratingsByBookId = loadAverageRatingsByBookId(bookIds);

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
                            ratingsByBookId.getOrDefault(book.getId(), 0.0),
                            coverUrl
                    );
                })
                .toList();

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
        Map<Integer, Double> ratingsByBookId = loadAverageRatingsByBookId(bookIds);

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
                            ratingsByBookId.getOrDefault(book.getId(), 0.0),
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
        Double avgRating = calculateAverageRating(book);
        Long totalRatings = getTotalRatings(book);
        List<CommentResponse> comments = mapComments(book.getComments());

        String coverUrl = book.getCoverImagePath() != null
                ? "/api/books/" + book.getId() + "/cover"
                : null;

        return new BookDetailResponse(
                book.getId(),
                book.getTitle(),
                book.getDescription(),
                authors,
                genres,
                avgRating > 0 ? avgRating : null,
                totalRatings,
                comments,
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

    protected Double calculateAverageRating(Book book) {
        if (book.getRatings() == null || book.getRatings().isEmpty()) return 0.0;
        return book.getRatings().stream()
                .mapToInt(Rating::getValue)
                .average()
                .orElse(0.0);
    }

    protected Long getTotalRatings(Book book) {
        if (book.getRatings() == null) return 0L;
        return (long) book.getRatings().size();
    }

    protected List<CommentResponse> mapComments(List<Comment> comments) {
        if (comments == null) return Collections.emptyList();
        return comments.stream()
                .filter(comment -> comment.getDeletedAt() == null)
                .map(comment -> new CommentResponse(
                        comment.getId(),
                        comment.getUser().getId(),
                        comment.getUser().getUsername(),
                        comment.getBook().getId(),
                        comment.getText(),
                        comment.getCreatedAt(),
                        comment.getDeletedAt()
                ))
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

    protected Map<Integer, Double> loadAverageRatingsByBookId(List<Integer> bookIds) {
        List<Rating> ratings = ratingRepository.findByBookIdIn(bookIds);
        return ratings.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getBook().getId(),
                        Collectors.averagingDouble(Rating::getValue)
                ));
    }
}