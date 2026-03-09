package org.nahap.bookcatalogservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.nahap.bookcatalogservice.configuration.BookStorageProperties;
import org.nahap.bookcatalogservice.dto.mapper.BookMapper;
import org.nahap.bookcatalogservice.dto.request.BookCreateRequest;
import org.nahap.bookcatalogservice.dto.request.BookUpdateRequest;
import org.nahap.bookcatalogservice.dto.response.BookDetailResponse;
import org.nahap.bookcatalogservice.dto.response.BookResponse;
import org.nahap.bookcatalogservice.entity.*;
import org.nahap.bookcatalogservice.exception.*;
import org.nahap.bookcatalogservice.repository.*;
import org.nahap.bookcatalogservice.service.BookCoverService;
import org.nahap.bookcatalogservice.service.BookService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
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

/**
 * Реализация сервиса для управления книгами
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final GenreRepository genreRepository;
    private final TagRepository tagRepository;
    private final BookAuthorRepository bookAuthorRepository;
    private final BookGenreRepository bookGenreRepository;
    private final BookTagRepository bookTagRepository;
    private final BookMapper bookMapper;
    private final BookStorageProperties storageProperties;
    private final BookCoverService bookCoverService;

    @Override
    public String getBookFb2Content(Integer bookId) {
        Book book = findBookOrThrow(bookId);
        Path fullPath = buildSafePath(resolveFilePath(book.getFilePath()));

        if (!Files.exists(fullPath) || !Files.isReadable(fullPath)) {
            throw new BookStorageException("Файл книги не найден или недоступен: " + fullPath);
        }

        // Пробуем кодировки: UTF-8 → Windows-1251 → ISO-8859-1 (старые русские FB2)
        java.nio.charset.Charset[] charsets = {
                StandardCharsets.UTF_8,
                java.nio.charset.Charset.forName("Windows-1251"),
                StandardCharsets.ISO_8859_1
        };
        for (java.nio.charset.Charset cs : charsets) {
            try {
                String content = Files.readString(fullPath, cs);
                // Убираем encoding из XML-декларации: контент уже в Unicode (Java String),
                // Spring отдаёт его как UTF-8, поэтому объявление encoding="windows-1251"
                // заставит браузер/foliate-js неверно декодировать байты → кракозябры.
                content = content.replaceAll(
                        "(<\\?xml[^?]*?)\\s+encoding\\s*=\\s*['\"][^'\"]*['\"]", "$1");
                return content;
            } catch (java.nio.charset.MalformedInputException ignored) {
                // попробуем следующую кодировку
            } catch (Exception e) {
                log.error("Ошибка чтения FB2-файла книги ID {}", bookId, e);
                throw new BookStorageException("Не удалось прочитать файл книги: " + e.getMessage());
            }
        }
        log.error("Не удалось определить кодировку FB2-файла книги ID {}", bookId);
        throw new BookStorageException("Не удалось определить кодировку файла книги ID " + bookId);
    }

    @Override
    public Path getBookFilePath(Integer bookId) {
        Book book = findBookOrThrow(bookId);
        return buildSafePath(resolveFilePath(book.getFilePath()));
    }

    @Override
    public Resource getBookAsResource(Integer bookId) {
        return new FileSystemResource(getBookFilePath(bookId));
    }

    @Override
    @Transactional
    public BookResponse createBook(BookCreateRequest request) {
        validateBookCreateRequest(request);

        Book book = new Book();
        book.setTitle(request.title());
        book.setDescription(request.description());
        book.setFilePath(request.filePath());
        if (request.publicationYear() != null) book.setPublicationYear(request.publicationYear());
        if (request.language()        != null) book.setLanguage(request.language());
        if (request.ageRating()       != null) book.setAgeRating(request.ageRating());
        if (request.seriesName()      != null) book.setSeriesName(request.seriesName());
        if (request.seriesNumber()    != null) book.setSeriesNumber(request.seriesNumber());
        if (request.wordCount()       != null) book.setWordCount(request.wordCount());
        book = bookRepository.save(book);

        extractAndSaveCoverIfPossible(book);

        saveBookAuthors(book, request.authorIds());
        saveBookGenres(book, request.genreIds());
        if (request.tagIds() != null && !request.tagIds().isEmpty()) {
            saveBookTags(book, request.tagIds());
        }

        return bookMapper.toResponseWithCover(book);
    }

    @Override
    @Transactional
    public BookResponse updateBook(Integer bookId, BookUpdateRequest request) {
        Book book = findBookOrThrow(bookId);

        book.setTitle(request.title());
        if (request.description() != null) {
            book.setDescription(request.description());
        }
        if (request.filePath() != null) {
            if (request.filePath().isBlank()) {
                throw new BookStorageException("Путь к файлу книги не может быть пустым");
            }
            book.setFilePath(request.filePath());
        }
        if (request.publicationYear() != null) book.setPublicationYear(request.publicationYear());
        if (request.language()        != null) book.setLanguage(request.language());
        if (request.ageRating()       != null) book.setAgeRating(request.ageRating());
        if (request.seriesName()      != null) book.setSeriesName(request.seriesName());
        if (request.seriesNumber()    != null) book.setSeriesNumber(request.seriesNumber());
        if (request.tagIds() != null) {
            bookTagRepository.deleteByBookId(bookId);
            if (!request.tagIds().isEmpty()) {
                saveBookTags(book, request.tagIds());
            }
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
        return bookMapper.toResponseWithCover(book);
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
            String sortField
    ) {
        // Сортировка теперь поддерживается через Pageable напрямую (title, averageRating, etc.)
        Page<Book> bookPage = bookRepository.searchBooksSimpleWithSort(title, authorIds, genreIds, pageable);

        return bookMapper.toResponsePageWithCovers(bookPage);
    }

    @Override
    @Transactional
    public void deleteBook(Integer bookId) {
        Book book = findBookOrThrow(bookId);

        if (book.getCoverImagePath() != null) {
            bookCoverService.deleteCover(book.getCoverImagePath());
        }

        bookRepository.deleteById(bookId);
        log.info("Удалена книга с ID: {}", bookId);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource getBookCover(Integer bookId) {
        Book book = findBookOrThrow(bookId);

        if (book.getCoverImagePath() == null) {
            return null;
        }

        Path coverPath = buildSafePath(book.getCoverImagePath());

        if (!Files.exists(coverPath)) {
            log.warn("Файл обложки не найден: {}", coverPath);
            return null;
        }

        return new FileSystemResource(coverPath);
    }

    @Override
    @Transactional(readOnly = true)
    public BookDetailResponse getBookDetails(Integer bookId) {
        Book book = findBookOrThrow(bookId);
        initializeBookRelations(book);
        return bookMapper.toDetailResponseWithCover(book);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BookResponse> getAllBooks(Pageable pageable) {
        Page<Book> bookPage = bookRepository.findAll(pageable);
        return bookMapper.toResponsePageWithCovers(bookPage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookResponse> getBooksByAuthorId(Integer authorId) {
        if (!authorRepository.existsById(authorId)) {
            throw new AuthorNotFoundException("Автор с ID " + authorId + " не найден");
        }

        List<Book> books = bookRepository.findByAuthorId(authorId);
        return bookMapper.toResponseListWithCovers(books);
    }

    private Book findBookOrThrow(Integer bookId) {
        return bookRepository.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException("Книга с ID " + bookId + " не найдена"));
    }

    private void validateBookCreateRequest(BookCreateRequest request) {
        if (!StringUtils.hasText(request.filePath())) {
            throw new BookStorageException("Путь к файлу книги не может быть пустым");
        }
        validateAuthorsExist(request.authorIds());
        validateGenresExist(request.genreIds());
    }

    private void validateAuthorsExist(List<Integer> authorIds) {
        List<Author> foundAuthors = authorRepository.findAllById(authorIds);
        if (foundAuthors.size() != authorIds.size()) {
            Set<Integer> foundIds = foundAuthors.stream()
                    .map(Author::getId)
                    .collect(Collectors.toSet());
            List<Integer> missing = authorIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .toList();
            throw new AuthorNotFoundException("Авторы с ID не найдены: " + missing);
        }
    }

    private void validateGenresExist(List<Integer> genreIds) {
        List<Genre> foundGenres = genreRepository.findAllById(genreIds);
        if (foundGenres.size() != genreIds.size()) {
            Set<Integer> foundIds = foundGenres.stream()
                    .map(Genre::getId)
                    .collect(Collectors.toSet());
            List<Integer> missing = genreIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .toList();
            throw new GenreNotFoundException("Жанры с ID не найдены: " + missing);
        }
    }

    private void extractAndSaveCoverIfPossible(Book book) {
        try {
            Path bookPath = buildSafePath(resolveFilePath(book.getFilePath()));
            if (!Files.exists(bookPath)) return;

            // Используем getBookFb2Content для поддержки fallback-кодировок (UTF-8, Win-1251, ISO-8859-1)
            String fb2Content = getBookFb2Content(book.getId());
            String coverPath = bookCoverService.extractAndSaveCover(fb2Content);

            if (coverPath != null) {
                book.setCoverImagePath(coverPath);
                bookRepository.save(book);
                log.info("Обложка извлечена и сохранена для книги ID {}", book.getId());
            }
        } catch (Exception e) {
            log.error("Ошибка извлечения обложки для книги ID {}", book.getId(), e);
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

        if (book.getBookGenres() != null) {
            book.getBookGenres().addAll(bookGenres);
        }
    }

    private void saveBookTags(Book book, List<Integer> tagIds) {
        List<Tag> tags = tagRepository.findAllById(tagIds);
        List<BookTag> bookTags = tags.stream()
                .map(tag -> {
                    BookTag bt = new BookTag();
                    bt.setBook(book);
                    bt.setTag(tag);
                    return bt;
                })
                .toList();
        bookTagRepository.saveAll(bookTags);

        if (book.getBookTags() != null) {
            book.getBookTags().addAll(bookTags);
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
        if (book.getBookTags() != null) {
            Hibernate.initialize(book.getBookTags());
            book.getBookTags().forEach(bt -> Hibernate.initialize(bt.getTag()));
        }
    }

    private String resolveFilePath(String filePath) {
        if (!StringUtils.hasText(filePath)) {
            throw new BookStorageException("Путь к файлу книги не указан");
        }
        return filePath.startsWith("/") ? filePath.substring(1) : filePath;
    }

    private Path buildSafePath(String relativePath) {
        Path basePath = Paths.get(storageProperties.getStoragePath())
                .normalize()
                .toAbsolutePath();
        Path fullPath = basePath.resolve(relativePath).normalize();

        if (!fullPath.startsWith(basePath)) {
            throw new SecurityException("Попытка доступа за пределы корневой директории хранилища");
        }
        return fullPath;
    }
}