package org.nahap.bookcatalogservice.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nahap.bookcatalogservice.entity.Author;
import org.nahap.bookcatalogservice.entity.Book;
import org.nahap.bookcatalogservice.entity.BookAuthor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class BookRepositoryIT {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private BookAuthorRepository bookAuthorRepository;

    private Author abgaryan;
    private Author akunin;
    private Book zulalyBook;
    private Book pelagiyaBook;
    private Book philosophyBook;

    @BeforeEach
    void setUp() {
        // Авторы
        abgaryan = authorRepository.save(new Author(null, "Наринэ", "Абгарян", null));
        akunin = authorRepository.save(new Author(null, "Борис", "Акунин", null));

        // Книги
        zulalyBook = bookRepository.save(buildBook("Зулали", "ru"));
        pelagiyaBook = bookRepository.save(buildBook("Пелагия и белый бульдог", "ru"));
        philosophyBook = bookRepository.save(buildBook("Философия жизни", "en"));

        // Связи книги-авторы
        bookAuthorRepository.save(new BookAuthor(null, zulalyBook, abgaryan));
        bookAuthorRepository.save(new BookAuthor(null, pelagiyaBook, akunin));
    }

    @Test
    void findByTitleContainingIgnoreCase_matchFound_returnsList() {
        List<Book> result = bookRepository.findByTitleContainingIgnoreCase("зулали");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Зулали");
    }

    @Test
    void findByTitleContainingIgnoreCase_noMatch_returnsEmpty() {
        List<Book> result = bookRepository.findByTitleContainingIgnoreCase("несуществующая");
        assertThat(result).isEmpty();
    }

    @Test
    void findByTitleContainingIgnoreCase_partialMatch_returnsAll() {
        // "Пелагия" и "Философия" — обе содержат "фил" если искать русское, но только Philosophy
        // Проверяем, что поиск регистронезависим
        List<Book> result = bookRepository.findByTitleContainingIgnoreCase("Белый");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).contains("бульдог");
    }

    @Test
    void findByAuthorId_authorWithBooks_returnsCorrectBooks() {
        List<Book> result = bookRepository.findByAuthorId(abgaryan.getId());
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Зулали");
    }

    @Test
    void findByAuthorId_authorWithoutBooks_returnsEmpty() {
        Author noBooks = authorRepository.save(new Author(null, "Без", "Книг", null));
        List<Book> result = bookRepository.findByAuthorId(noBooks.getId());
        assertThat(result).isEmpty();
    }

    @Test
    void searchBooksSimple_byTitle_returnsMatch() {
        Page<Book> result = bookRepository.searchBooksSimple(
                "зулали", null, null, PageRequest.of(0, 10)
        );
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Зулали");
    }

    @Test
    void searchBooksSimple_byAuthorId_returnsMatch() {
        Page<Book> result = bookRepository.searchBooksSimple(
                null, List.of(akunin.getId()), null, PageRequest.of(0, 10)
        );
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Пелагия и белый бульдог");
    }

    @Test
    void searchBooksSimple_noFilters_returnsAll() {
        Page<Book> result = bookRepository.searchBooksSimple(
                null, null, null, PageRequest.of(0, 10)
        );
        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    @Test
    void findAll_returnsAllBooks() {
        List<Book> all = bookRepository.findAll();
        assertThat(all).hasSize(3);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Book buildBook(String title, String language) {
        Book book = new Book();
        book.setTitle(title);
        book.setFilePath("/" + title + ".fb2");
        book.setLanguage(language);
        book.setRatingsCount(0);
        book.setAverageRating(BigDecimal.ZERO);
        return book;
    }
}
