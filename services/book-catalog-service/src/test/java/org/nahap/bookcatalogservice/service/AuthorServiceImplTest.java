package org.nahap.bookcatalogservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nahap.bookcatalogservice.dto.mapper.AuthorMapper;
import org.nahap.bookcatalogservice.dto.response.AuthorResponse;
import org.nahap.bookcatalogservice.entity.Author;
import org.nahap.bookcatalogservice.exception.AuthorHasBooksException;
import org.nahap.bookcatalogservice.exception.AuthorNotFoundException;
import org.nahap.bookcatalogservice.exception.InvalidAuthorNameException;
import org.nahap.bookcatalogservice.repository.AuthorRepository;
import org.nahap.bookcatalogservice.service.impl.AuthorServiceImpl;
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
class AuthorServiceImplTest {

    @Mock private AuthorRepository authorRepository;
    @Mock private AuthorMapper authorMapper;

    @InjectMocks
    private AuthorServiceImpl authorService;

    private Author testAuthor;
    private AuthorResponse testAuthorResponse;

    @BeforeEach
    void setUp() {
        testAuthor = new Author();
        testAuthor.setId(1);
        testAuthor.setFirstName("Иван");
        testAuthor.setLastName("Иванов");

        testAuthorResponse = new AuthorResponse(1, "Иван", "Иванов", "Иван Иванов");
    }

    @Test
    void createAuthor_validNames_returnsAuthorResponse() {
        when(authorRepository.save(any(Author.class))).thenReturn(testAuthor);
        when(authorMapper.toResponse(testAuthor)).thenReturn(testAuthorResponse);

        AuthorResponse result = authorService.createAuthor("Иван", "Иванов");

        assertThat(result).isNotNull();
        assertThat(result.firstName()).isEqualTo("Иван");
        assertThat(result.lastName()).isEqualTo("Иванов");
        assertThat(result.fullName()).isEqualTo("Иван Иванов");
        verify(authorRepository).save(any(Author.class));
    }

    @Test
    void createAuthor_blankFirstName_throwsInvalidAuthorNameException() {
        assertThatThrownBy(() -> authorService.createAuthor("  ", "Иванов"))
                .isInstanceOf(InvalidAuthorNameException.class);

        verifyNoInteractions(authorRepository);
    }

    @Test
    void createAuthor_nullLastName_throwsInvalidAuthorNameException() {
        assertThatThrownBy(() -> authorService.createAuthor("Иван", null))
                .isInstanceOf(InvalidAuthorNameException.class);
    }

    @Test
    void getAuthorById_whenFound_returnsAuthorResponse() {
        when(authorRepository.findById(1)).thenReturn(Optional.of(testAuthor));
        when(authorMapper.toResponse(testAuthor)).thenReturn(testAuthorResponse);

        AuthorResponse result = authorService.getAuthorById(1);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1);
    }

    @Test
    void getAuthorById_whenNotFound_throwsAuthorNotFoundException() {
        when(authorRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authorService.getAuthorById(99))
                .isInstanceOf(AuthorNotFoundException.class);
    }

    @Test
    void searchAuthors_withQuery_returnsFilteredPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Author> authorPage = new PageImpl<>(List.of(testAuthor));

        when(authorRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
                "Иван", "Иван", pageable)).thenReturn(authorPage);

        Page<AuthorResponse> result = authorService.searchAuthors("Иван", pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).firstName()).isEqualTo("Иван");
    }

    @Test
    void searchAuthors_withNullQuery_returnsAllAuthors() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Author> authorPage = new PageImpl<>(List.of(testAuthor));

        when(authorRepository.findAll(pageable)).thenReturn(authorPage);

        Page<AuthorResponse> result = authorService.searchAuthors(null, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void deleteAuthor_whenNotFound_throwsAuthorNotFoundException() {
        when(authorRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authorService.deleteAuthor(99))
                .isInstanceOf(AuthorNotFoundException.class);

        verify(authorRepository, never()).delete(any());
    }

    @Test
    void deleteAuthor_whenHasBooks_throwsAuthorHasBooksException() {
        when(authorRepository.findById(1)).thenReturn(Optional.of(testAuthor));
        when(authorRepository.hasBooks(1)).thenReturn(true);

        assertThatThrownBy(() -> authorService.deleteAuthor(1))
                .isInstanceOf(AuthorHasBooksException.class);

        verify(authorRepository, never()).delete(any());
    }

    @Test
    void deleteAuthor_whenExistsWithNoBooks_deletesAuthor() {
        when(authorRepository.findById(1)).thenReturn(Optional.of(testAuthor));
        when(authorRepository.hasBooks(1)).thenReturn(false);

        authorService.deleteAuthor(1);

        verify(authorRepository).delete(testAuthor);
    }
}
