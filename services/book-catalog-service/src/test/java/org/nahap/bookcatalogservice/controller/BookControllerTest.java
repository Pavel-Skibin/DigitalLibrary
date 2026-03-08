package org.nahap.bookcatalogservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.nahap.bookcatalogservice.client.UserTrackingClient;
import org.nahap.bookcatalogservice.configuration.SecurityConfig;
import org.nahap.bookcatalogservice.dto.request.BookCreateRequest;
import org.nahap.bookcatalogservice.dto.response.BookDetailResponse;
import org.nahap.bookcatalogservice.dto.response.BookResponse;
import org.nahap.bookcatalogservice.exception.BookNotFoundException;
import org.nahap.bookcatalogservice.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookController.class)
@Import(SecurityConfig.class)
class BookControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean BookService bookService;
    @MockBean UserTrackingClient userTrackingClient;

    private BookResponse buildBookResponse(int id, String title) {
        return new BookResponse(id, title, "Описание",
                List.of("Автор"), List.of("Жанр"),
                4.0, 5, null, 50000, "ru", 2020, "12+", null, null, List.of());
    }

    @Test
    void getAllBooks_returns200WithPage() throws Exception {
        var page = new PageImpl<>(List.of(buildBookResponse(1, "Книга 1")));
        when(bookService.getAllBooks(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Книга 1"));
    }

    @Test
    void getBookDetails_whenFound_returns200() throws Exception {
        BookDetailResponse detail = new BookDetailResponse(
                1, "Книга 1", "Описание",
                List.of("Автор"), List.of("Жанр"),
                4.0, 5, null, 50000, "ru", 2020, "12+", null, null, List.of()
        );
        when(bookService.getBookDetails(1)).thenReturn(detail);

        mockMvc.perform(get("/api/books/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Книга 1"));
    }

    @Test
    void getBookDetails_whenNotFound_returns404() throws Exception {
        when(bookService.getBookDetails(99)).thenThrow(new BookNotFoundException("Книга не найдена"));

        mockMvc.perform(get("/api/books/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteBook_returns204() throws Exception {
        doNothing().when(bookService).deleteBook(1);

        mockMvc.perform(delete("/api/books/1"))
                .andExpect(status().isNoContent());

        verify(bookService).deleteBook(1);
    }

    @Test
    void createBook_validRequest_returns201() throws Exception {
        BookCreateRequest request = new BookCreateRequest(
                "Новая книга", "Описание", "/books/test.fb2",
                List.of(1), List.of(1),
                2023, "ru", "12+", null, null, 30000, List.of()
        );
        BookResponse response = buildBookResponse(2, "Новая книга");
        when(bookService.createBook(any(BookCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.title").value("Новая книга"));
    }

    @Test
    void searchBooks_returns200WithResults() throws Exception {
        var page = new PageImpl<>(List.of(buildBookResponse(1, "Найденная книга")));
        when(bookService.searchBooks(any(), any(), any(), any(), any(), any(Pageable.class), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/books/search")
                        .param("title", "Найденная"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Найденная книга"));
    }
}
