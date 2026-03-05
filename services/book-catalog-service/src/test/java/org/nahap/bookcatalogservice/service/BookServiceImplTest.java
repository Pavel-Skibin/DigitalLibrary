package org.nahap.bookcatalogservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nahap.bookcatalogservice.dto.mapper.BookMapper;
import org.nahap.bookcatalogservice.dto.request.BookCreateRequest;
import org.nahap.bookcatalogservice.dto.response.BookDetailResponse;
import org.nahap.bookcatalogservice.dto.response.BookResponse;
import org.nahap.bookcatalogservice.entity.Author;
import org.nahap.bookcatalogservice.entity.Book;
import org.nahap.bookcatalogservice.entity.Genre;
import org.nahap.bookcatalogservice.exception.AuthorNotFoundException;
import org.nahap.bookcatalogservice.exception.BookNotFoundException;
import org.nahap.bookcatalogservice.repository.*;
import org.nahap.bookcatalogservice.configuration.BookStorageProperties;
import org.nahap.bookcatalogservice.service.impl.BookServiceImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceImplTest {

    @Mock private BookRepository bookRepository;
    @Mock private AuthorRepository authorRepository;
    @Mock private GenreRepository genreRepository;
    @Mock private TagRepository tagRepository;
    @Mock private BookAuthorRepository bookAuthorRepository;
    @Mock private BookGenreRepository bookGenreRepository;
    @Mock private BookTagRepository bookTagRepository;
    @Mock private BookMapper bookMapper;
    @Mock private BookStorageProperties storageProperties;
    @Mock private BookCoverService bookCoverService;

    @InjectMocks
    private BookServiceImpl bookService;

    private Book testBook;
    private BookResponse testBookResponse;
    private BookDetailResponse testBookDetailResponse;

    @BeforeEach
    void setUp() {
        testBook = new Book();
        testBook.setId(1);
        testBook.setTitle("Тестовая книга");
        testBook.setFilePath("/books/test.fb2");

        testBookResponse = new BookResponse(
                1, "Тестовая книга", "Описание",
                List.of("Иванов Иван"), List.of("Фантастика"),
                4.5, 10, null, 50000, "ru", 2020, "16+", null, null, List.of()
        );

        testBookDetailResponse = new BookDetailResponse(
                1, "Тестовая книга", "Описание",
                List.of("Иванов Иван"), List.of("Фантастика"),
                4.5, 10, null, 50000, "ru", 2020, "16+", null, null, List.of()
        );
    }

    @Test
    void getAllBooks_returnsPageOfBooks() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Book> bookPage = new PageImpl<>(List.of(testBook));
        Page<BookResponse> expectedPage = new PageImpl<>(List.of(testBookResponse));

        when(bookRepository.findAll(pageable)).thenReturn(bookPage);
        when(bookMapper.toResponsePageWithCovers(bookPage)).thenReturn(expectedPage);

        Page<BookResponse> result = bookService.getAllBooks(pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).title()).isEqualTo("Тестовая книга");
        verify(bookRepository).findAll(pageable);
    }

    @Test
    void getBookDetails_whenFound_returnsDetailResponse() {
        when(bookRepository.findById(1)).thenReturn(Optional.of(testBook));
        when(bookMapper.toDetailResponseWithCover(testBook)).thenReturn(testBookDetailResponse);

        BookDetailResponse result = bookService.getBookDetails(1);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1);
        assertThat(result.title()).isEqualTo("Тестовая книга");
    }

    @Test
    void getBookDetails_whenNotFound_throwsBookNotFoundException() {
        when(bookRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.getBookDetails(99))
                .isInstanceOf(BookNotFoundException.class);
    }

    @Test
    void deleteBook_whenFound_deletesBook() {
        when(bookRepository.findById(1)).thenReturn(Optional.of(testBook));

        bookService.deleteBook(1);

        verify(bookRepository).deleteById(1);
    }

    @Test
    void deleteBook_whenNotFound_throwsBookNotFoundException() {
        when(bookRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.deleteBook(99))
                .isInstanceOf(BookNotFoundException.class);
    }

    @Test
    void createBook_validRequest_savesAndReturnsResponse() {
        BookCreateRequest request = new BookCreateRequest(
                "Новая книга", "Описание", "/books/new.fb2",
                List.of(1), List.of(1),
                2023, "ru", "12+", null, null, 30000, List.of()
        );

        Author author = new Author();
        author.setId(1);
        author.setFirstName("Иван");
        author.setLastName("Иванов");

        Genre genre = new Genre();
        genre.setId(1);
        genre.setName("Фантастика");

        Book savedBook = new Book();
        savedBook.setId(2);
        savedBook.setTitle("Новая книга");
        savedBook.setFilePath("/books/new.fb2");

        BookResponse expectedResponse = new BookResponse(
                2, "Новая книга", "Описание",
                List.of("Иванов Иван"), List.of("Фантастика"),
                0.0, 0, null, 30000, "ru", 2023, "12+", null, null, List.of()
        );

        when(authorRepository.findAllById(List.of(1))).thenReturn(List.of(author));
        when(genreRepository.findAllById(List.of(1))).thenReturn(List.of(genre));
        when(bookRepository.save(any(Book.class))).thenReturn(savedBook);
        when(bookMapper.toResponseWithCover(savedBook)).thenReturn(expectedResponse);

        BookResponse result = bookService.createBook(request);

        assertThat(result).isNotNull();
        assertThat(result.title()).isEqualTo("Новая книга");
        verify(bookRepository, atLeastOnce()).save(any(Book.class));
    }

    @Test
    void getBooksByAuthorId_whenAuthorNotFound_throwsException() {
        when(authorRepository.existsById(99)).thenReturn(false);

        assertThatThrownBy(() -> bookService.getBooksByAuthorId(99))
                .isInstanceOf(AuthorNotFoundException.class);
    }
}
