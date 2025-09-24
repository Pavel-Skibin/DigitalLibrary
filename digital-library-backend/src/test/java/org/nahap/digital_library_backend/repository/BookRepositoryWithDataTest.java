package org.nahap.digital_library_backend.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.nahap.digital_library_backend.entity.Book;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@Tag("db")
@DisplayName("BookRepository Tests with Real Data")
@Sql(scripts = "/sql/complete-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class BookRepositoryWithDataTest extends BaseRepositoryTest {

    @Autowired
    private BookRepository bookRepository;

    @Test
    @DisplayName("Should find books by title containing 'Зулали' (case-insensitive)")
    void shouldFindBooksByTitleContainingIgnoreCase() {
        List<Book> books = bookRepository.findByTitleContainingIgnoreCase("зулали");
        assertThat(books).hasSize(1);
        assertThat(books.get(0).getTitle()).isEqualTo("Зулали");
    }

    @Test
    @DisplayName("Should find books by genre ID 5 (Фантастика)")
    void shouldFindBooksByGenreId_Fantasy() {
        List<Book> books = bookRepository.findByGenreId(5);
        assertThat(books).hasSize(2);
        assertThat(books).extracting(Book::getTitle)
                .contains(
                        "Четвертый ледниковый период",
                        "Сокровища Валькирии. Стоящий у Солнца"
                );
    }
    @Test
    @DisplayName("Should find books by author ID 1 (Наринэ Абгарян)")
    void shouldFindBooksByAuthorId_Abgarjan() {
        List<Book> books = bookRepository.findByAuthorId(1);
        assertThat(books).hasSize(3);
        assertThat(books).extracting(Book::getTitle)
                .contains("Зулали", "Понаехавшая", "С неба упали три яблока");
    }

    @Test
    @DisplayName("Should paginate all books")
    void shouldPaginateAllBooks() {
        var page = bookRepository.findAll(PageRequest.of(0, 5));
        assertThat(page.getContent()).hasSize(5);
        assertThat(page.getTotalElements()).isEqualTo(10);
    }

//    @Test
//    @DisplayName("Should find detailed book by ID with all associations")
//    void shouldFindDetailedBookById() {
//        Optional<Book> bookOpt = bookRepository.findDetailedById(1);
//        assertThat(bookOpt).isPresent();
//        Book book = bookOpt.get();
//
//        assertThat(book.getBookAuthors()).isNotEmpty();
//        assertThat(book.getBookAuthors().get(0).getAuthor().getFirstName()).isEqualTo("Наринэ");
//
//        assertThat(book.getBookGenres()).isNotEmpty();
//        assertThat(book.getBookGenres()).extracting(bg -> bg.getGenre().getName())
//                .contains("Современная проза", "Драма", "Юмор");
//
//        assertThat(book.getRatings()).hasSize(4); // из SQL
//        assertThat(book.getComments()).hasSize(3); // активные комментарии (id 1,2,3; id 20 удалён)
//    }
//
//    @Test
//    @DisplayName("Should return empty for non-existing book ID")
//    void shouldReturnEmptyForNonExistingBook() {
//        Optional<Book> book = bookRepository.findDetailedById(999);
//        assertThat(book).isEmpty();
//    }
}