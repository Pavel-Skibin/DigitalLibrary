package org.nahap.bookcatalogservice.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nahap.bookcatalogservice.entity.Author;
import org.nahap.bookcatalogservice.entity.Book;
import org.nahap.bookcatalogservice.entity.BookAuthor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class AuthorRepositoryIT {

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookAuthorRepository bookAuthorRepository;

    private Author abgaryan;
    private Author akunin;
    private Author noBooks;

    @BeforeEach
    void setUp() {
        abgaryan = authorRepository.save(new Author(null, "Наринэ", "Абгарян", null));
        akunin = authorRepository.save(new Author(null, "Борис", "Акунин", null));
        noBooks = authorRepository.save(new Author(null, "Без", "Книг", null));

        Book book1 = bookRepository.save(buildBook("Зулали"));
        Book book2 = bookRepository.save(buildBook("Пелагия"));

        bookAuthorRepository.save(new BookAuthor(null, book1, abgaryan));
        bookAuthorRepository.save(new BookAuthor(null, book2, abgaryan));
        bookAuthorRepository.save(new BookAuthor(null, book2, akunin));
    }

    @Test
    void findByLastNameContainingIgnoreCase_match_returnsList() {
        List<Author> result = authorRepository.findByLastNameContainingIgnoreCase("абгарян");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFirstName()).isEqualTo("Наринэ");
    }

    @Test
    void findByFirstNameContainingIgnoreCase_noMatch_returnsEmpty() {
        List<Author> result = authorRepository.findByFirstNameContainingIgnoreCase("Иван");
        assertThat(result).isEmpty();
    }

    @Test
    void findByFirstOrLastName_nameOrLastNameSearch_returnsMatches() {
        var page = authorRepository
                .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
                        "борис", "борис", PageRequest.of(0, 10));
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getLastName()).isEqualTo("Акунин");
    }

    @Test
    void hasBooks_authorWithBooks_returnsTrue() {
        assertThat(authorRepository.hasBooks(abgaryan.getId())).isTrue();
    }

    @Test
    void hasBooks_authorWithoutBooks_returnsFalse() {
        assertThat(authorRepository.hasBooks(noBooks.getId())).isFalse();
    }

    @Test
    void countBooksByAuthorId_returnsCorrectCount() {
        Long count = authorRepository.countBooksByAuthorId(abgaryan.getId());
        assertThat(count).isEqualTo(2L);
    }

    @Test
    void findTopAuthorsByBookCount_returnsOrderedByBookCount() {
        var page = authorRepository.findTopAuthorsByBookCount(PageRequest.of(0, 10));
        assertThat(page.getContent()).isNotEmpty();
        // Абгарян должен быть первым (2 книги)
        Object[] topRow = page.getContent().get(0);
        Long topCount = (Long) topRow[3];
        assertThat(topCount).isEqualTo(2L);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Book buildBook(String title) {
        Book book = new Book();
        book.setTitle(title);
        book.setFilePath("/" + title + ".fb2");
        book.setLanguage("ru");
        book.setRatingsCount(0);
        book.setAverageRating(BigDecimal.ZERO);
        return book;
    }
}
