package org.nahap.digital_library_backend.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.nahap.digital_library_backend.configuration.BookStorageProperties;
import org.nahap.digital_library_backend.dto.mapper.BookMapper;
import org.nahap.digital_library_backend.entity.*;
import org.nahap.digital_library_backend.exception.*;
import org.nahap.digital_library_backend.repository.*;
import org.nahap.digital_library_backend.dto.request.BookCreateRequest;
import org.nahap.digital_library_backend.dto.request.BookUpdateRequest;
import org.nahap.digital_library_backend.dto.response.BookDetailResponse;
import org.nahap.digital_library_backend.dto.response.BookResponse;
import org.nahap.digital_library_backend.service.BookService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final GenreRepository genreRepository;
    private final BookAuthorRepository bookAuthorRepository;
    private final BookGenreRepository bookGenreRepository;
    private final RatingRepository ratingRepository;
    private final BookMapper bookMapper;
    private final BookStorageProperties storageProperties;

    // === ФАЙЛОВЫЕ ОПЕРАЦИИ ===

    @Override
    public String getBookFb2Content(Integer bookId) {
        Book book = findBookOrThrow(bookId);
        String filePath = resolveFilePath(book.getFilePath());
        Path fullPath = buildSafePath(filePath);

        if (!Files.exists(fullPath) || !Files.isReadable(fullPath)) {
            throw new BookStorageException("Файл книги не найден или недоступен: " + fullPath);
        }

        try {
            log.info("Чтение FB2-файла книги ID {}: {}", bookId, fullPath);
            return Files.readString(fullPath, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Ошибка чтения FB2-файла книги ID {}: {}", bookId, fullPath, e);
            throw new BookStorageException("Не удалось прочитать файл книги: " + e.getMessage());
        }
    }

    @Override
    public Path getBookFilePath(Integer bookId) {
        Book book = findBookOrThrow(bookId);
        String filePath = resolveFilePath(book.getFilePath());
        return buildSafePath(filePath);
    }

    @Override
    public Resource getBookAsResource(Integer bookId) {
        Path path = getBookFilePath(bookId);
        return new FileSystemResource(path);
    }

    // === CRUD ===

    @Override
    @Transactional
    public BookResponse createBook(BookCreateRequest request) {

        if (!StringUtils.hasText(request.filePath())) {
            throw new BookStorageException("Путь к файлу книги не может быть пустым");
        }
        validateAuthorsExist(request.authorIds());
        validateGenresExist(request.genreIds());

        Book book = new Book();
        book.setTitle(request.title());
        book.setDescription(request.description());
        book.setFilePath(request.filePath());

        book = bookRepository.save(book);
        log.info("Создана новая книга с ID {}: {}", book.getId(), book.getTitle());

        saveBookAuthors(book, request.authorIds());
        saveBookGenres(book, request.genreIds());

        // Возвращаем корректно заполненный ответ с авторами и жанрами
        List<Integer> createdBookIdList = Collections.singletonList(book.getId());
        Map<Integer, List<String>> authorNamesByBookId = loadAuthorNamesByBookId(createdBookIdList);
        Map<Integer, List<String>> genreNamesByBookId = loadGenreNamesByBookId(createdBookIdList);

        List<String> authors = authorNamesByBookId.getOrDefault(book.getId(), Collections.emptyList());
        List<String> genres = genreNamesByBookId.getOrDefault(book.getId(), Collections.emptyList());

        return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getDescription(),
                authors,
                genres,
                null
        );
    }

    @Override
    @Transactional
    public BookResponse updateBook(Integer bookId, BookUpdateRequest request) {
        if (request.filePath() != null && !StringUtils.hasText(request.filePath())) {
            throw new BookStorageException("Путь к файлу книги не может быть пустым");
        }

        Book book = findBookOrThrow(bookId);

        book.setTitle(request.title());
        if (request.description() != null) {
            book.setDescription(request.description());
        }
        if (request.filePath() != null && !request.filePath().isBlank()) {
            book.setFilePath(request.filePath());
        }

        if (request.authorIds() != null) {
            bookAuthorRepository.deleteByBookId(bookId);
            if (!request.authorIds().isEmpty()) {
                validateAuthorsExist(request.authorIds());
                saveBookAuthors(book, request.authorIds());
            }
        }

        if (request.genreIds() != null) {
            bookGenreRepository.deleteByBookId(bookId);
            if (!request.genreIds().isEmpty()) {
                validateGenresExist(request.genreIds());
                saveBookGenres(book, request.genreIds());
            }
        }

        book = bookRepository.save(book);
        log.info("Обновлена книга с ID {}: {}", book.getId(), book.getTitle());
        return bookMapper.toResponse(book);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookResponse> searchBooks(
            String title,
            List<Integer> authorIds,
            List<Integer> genreIds,
            Double minRating,
            Double maxRating,
            Pageable pageable,
            String sortField // ← новое
    ) {
        boolean hasRatingFilter = (minRating != null && !minRating.isNaN()) || (maxRating != null && !maxRating.isNaN());
        boolean sortByRating = "rating".equals(sortField);

        Page<Book> bookPage;

        if (hasRatingFilter || sortByRating) {
            bookPage = bookRepository.searchBooksWithRatingAndSort(
                    title,
                    authorIds,
                    genreIds,
                    minRating,
                    maxRating,
                    sortField,
                    pageable
            );
        } else {
            bookPage = bookRepository.searchBooksSimpleWithSort(
                    title,
                    authorIds,
                    genreIds,
                    pageable
            );
        }

        if (bookPage.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        List<Integer> bookIds = bookPage.getContent().stream()
                .map(Book::getId)
                .toList();

        Map<Integer, List<String>> authorNamesByBookId = loadAuthorNamesByBookId(bookIds);
        Map<Integer, List<String>> genreNamesByBookId = loadGenreNamesByBookId(bookIds);
        Map<Integer, Double> avgRatingsByBookId = loadAverageRatingsByBookId(bookIds);

        List<BookResponse> bookResponses = bookPage.getContent().stream().map(book -> {
            List<String> authors = authorNamesByBookId.getOrDefault(book.getId(), Collections.emptyList());
            List<String> genres = genreNamesByBookId.getOrDefault(book.getId(), Collections.emptyList());
            Double avgRating = avgRatingsByBookId.getOrDefault(book.getId(), 0.0);

            return new BookResponse(
                    book.getId(),
                    book.getTitle(),
                    book.getDescription(),
                    authors,
                    genres,
                    avgRating > 0 ? avgRating : null
            );
        }).toList();

        return new PageImpl<>(bookResponses, pageable, bookPage.getTotalElements());
    }

    @Override
    @Transactional
    public void deleteBook(Integer bookId) {
        if (!bookRepository.existsById(bookId)) {
            throw new BookNotFoundException("Книга с ID " + bookId + " не найдена");
        }
        bookRepository.deleteById(bookId);
        log.info("Удалена книга с ID: {}", bookId);
    }

    // === ПОЛУЧЕНИЕ ДАННЫХ ===

    @Override
    @Transactional(readOnly = true)
    public BookDetailResponse getBookDetails(Integer bookId) {
        Book book = findBookOrThrow(bookId);

        // Инициализация связей для избежания LazyInitializationException
        initializeBookRelations(book);

        return bookMapper.toDetailResponse(book);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookResponse> getAllBooks(Pageable pageable) {
        Page<Book> bookPage = bookRepository.findAll(pageable);
        List<Book> books = bookPage.getContent();

        if (books.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        List<Integer> bookIds = books.stream().map(Book::getId).toList();

        // Batch-загрузка связей
        Map<Integer, List<String>> authorNamesByBookId = loadAuthorNamesByBookId(bookIds);
        Map<Integer, List<String>> genreNamesByBookId = loadGenreNamesByBookId(bookIds);
        Map<Integer, Double> avgRatingsByBookId = loadAverageRatingsByBookId(bookIds);

        List<BookResponse> responses = books.stream().map(book -> {
            List<String> authors = authorNamesByBookId.getOrDefault(book.getId(), Collections.emptyList());
            List<String> genres = genreNamesByBookId.getOrDefault(book.getId(), Collections.emptyList());
            Double avgRating = avgRatingsByBookId.getOrDefault(book.getId(), 0.0);

            return new BookResponse(
                    book.getId(),
                    book.getTitle(),
                    book.getDescription(),
                    authors,
                    genres,
                    avgRating > 0 ? avgRating : null
            );
        }).toList();

        return new PageImpl<>(responses, pageable, bookPage.getTotalElements());
    }


    @Override
    @Transactional(readOnly = true)
    public List<BookResponse> getBooksByAuthorId(Integer authorId) {
        if (!authorRepository.existsById(authorId)) {
            throw new AuthorNotFoundException("Автор с ID " + authorId + " не найден");
        }

        List<Book> books = bookRepository.findByAuthorId(authorId);
        if (books.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> bookIds = books.stream().map(Book::getId).toList();
        Map<Integer, List<String>> authorNamesByBookId = loadAuthorNamesByBookId(bookIds);
        Map<Integer, List<String>> genreNamesByBookId = loadGenreNamesByBookId(bookIds);
        Map<Integer, Double> avgRatingsByBookId = loadAverageRatingsByBookId(bookIds);

        return books.stream().map(book -> {
            List<String> authors = authorNamesByBookId.getOrDefault(book.getId(), Collections.emptyList());
            List<String> genres = genreNamesByBookId.getOrDefault(book.getId(), Collections.emptyList());
            Double avgRating = avgRatingsByBookId.getOrDefault(book.getId(), 0.0);

            return new BookResponse(
                    book.getId(),
                    book.getTitle(),
                    book.getDescription(),
                    authors,
                    genres,
                    avgRating > 0 ? avgRating : null
            );
        }).toList();
    }

    // === ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ===

    private Book findBookOrThrow(Integer bookId) {
        return bookRepository.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException("Книга с ID " + bookId + " не найдена"));
    }

    private void validateAuthorsExist(List<Integer> authorIds) {
        List<Author> foundAuthors = authorRepository.findAllById(authorIds);
        if (foundAuthors.size() != authorIds.size()) {
            Set<Integer> foundIds = foundAuthors.stream().map(Author::getId).collect(Collectors.toSet());
            List<Integer> missing = authorIds.stream().filter(id -> !foundIds.contains(id)).toList();
            throw new AuthorNotFoundException("Авторы с ID не найдены: " + missing);
        }
    }

    private void validateGenresExist(List<Integer> genreIds) {
        List<Genre> foundGenres = genreRepository.findAllById(genreIds);
        if (foundGenres.size() != genreIds.size()) {
            Set<Integer> foundIds = foundGenres.stream().map(Genre::getId).collect(Collectors.toSet());
            List<Integer> missing = genreIds.stream().filter(id -> !foundIds.contains(id)).toList();
            throw new GenreNotFoundException("Жанры с ID не найдены: " + missing);
        }
    }

    private void saveBookAuthors(Book book, List<Integer> authorIds) {
        List<Author> authors = authorRepository.findAllById(authorIds);
        List<BookAuthor> bookAuthors = authors.stream()
                .map(author -> {
                    BookAuthor ba = new BookAuthor();
                    ba.setBook(book);
                    ba.setAuthor(author);
                    return ba;
                })
                .toList();
        bookAuthorRepository.saveAll(bookAuthors);
        // синхронизируем двунаправленную связь в текущем persistence context
        if (book.getBookAuthors() != null) {
            book.getBookAuthors().addAll(bookAuthors);
        }
    }

    private void saveBookGenres(Book book, List<Integer> genreIds) {
        List<Genre> genres = genreRepository.findAllById(genreIds);
        List<BookGenre> bookGenres = genres.stream()
                .map(genre -> {
                    BookGenre bg = new BookGenre();
                    bg.setBook(book);
                    bg.setGenre(genre);
                    return bg;
                })
                .toList();
        bookGenreRepository.saveAll(bookGenres);
        // синхронизируем двунаправленную связь в текущем persistence context
        if (book.getBookGenres() != null) {
            book.getBookGenres().addAll(bookGenres);
        }
    }

    private void initializeBookRelations(Book book) {
        if (book.getBookAuthors() != null) {
            Hibernate.initialize(book.getBookAuthors());
            book.getBookAuthors().forEach(ba -> Hibernate.initialize(ba.getAuthor()));
        }
        if (book.getBookGenres() != null) {
            Hibernate.initialize(book.getBookGenres());
            book.getBookGenres().forEach(bg -> Hibernate.initialize(bg.getGenre()));
        }
        Hibernate.initialize(book.getRatings());
        if (book.getComments() != null) {
            Hibernate.initialize(book.getComments());
            book.getComments().forEach(c -> Hibernate.initialize(c.getUser()));
        }
    }

    private String resolveFilePath(String filePath) {
        if (!StringUtils.hasText(filePath)) {
            throw new BookStorageException("Путь к файлу книги не указан");
        }
        return filePath.startsWith("/") ? filePath.substring(1) : filePath;
    }

    private Path buildSafePath(String relativePath) {
        Path basePath = Paths.get(storageProperties.getStoragePath()).normalize().toAbsolutePath();
        Path fullPath = basePath.resolve(relativePath).normalize();

        if (!fullPath.startsWith(basePath)) {
            throw new SecurityException("Попытка доступа за пределы корневой директории хранилища");
        }
        return fullPath;
    }

    private Map<Integer, List<String>> loadAuthorNamesByBookId(List<Integer> bookIds) {
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

    private Map<Integer, List<String>> loadGenreNamesByBookId(List<Integer> bookIds) {
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

    private Map<Integer, Double> loadAverageRatingsByBookId(List<Integer> bookIds) {
        List<Rating> ratings = ratingRepository.findByBookIdIn(bookIds);
        return ratings.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getBook().getId(),
                        Collectors.averagingDouble(Rating::getValue)
                ));
    }
}