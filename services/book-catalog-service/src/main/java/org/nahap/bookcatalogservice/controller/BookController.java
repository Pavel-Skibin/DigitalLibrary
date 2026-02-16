package org.nahap.bookcatalogservice.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nahap.bookcatalogservice.dto.request.BookCreateRequest;
import org.nahap.bookcatalogservice.dto.request.BookUpdateRequest;
import org.nahap.bookcatalogservice.dto.response.BookDetailResponse;
import org.nahap.bookcatalogservice.dto.response.BookResponse;
import org.nahap.bookcatalogservice.service.BookService;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    // Гость: GET /api/books — список всех книг с пагинацией и сортировкой
    @GetMapping
    @Parameters({
            @Parameter(name = "page", description = "Номер страницы", example = "0"),
            @Parameter(name = "size", description = "Размер страницы", example = "10"),
            @Parameter(name = "sort", description = "Сортировка: property,direction", example = "title,asc")
    })
    public ResponseEntity<Page<BookResponse>> getAllBooks(Pageable pageable) {
        log.info("Запрос списка книг с пагинацией: {}", pageable);
        Page<BookResponse> books = bookService.getAllBooks(pageable);
        return ResponseEntity.ok(books);
    }

    // Гость: GET /api/books/{id} — детали книги
    @GetMapping("/{id}")
    public ResponseEntity<BookDetailResponse> getBookDetails(@PathVariable Integer id) {
        log.info("Запрос деталей книги ID: {}", id);
        BookDetailResponse response = bookService.getBookDetails(id);
        return ResponseEntity.ok(response);
    }

    // Гость: GET /api/books/{bookId}/fb2 — содержимое книги в формате FB2 (XML)
    @GetMapping("/{bookId}/fb2")
    public ResponseEntity<String> getBookFb2(@PathVariable Integer bookId) {
        try {
            log.info("Запрос на получение FB2 контента для книги ID: {}", bookId);
            String fb2Content = bookService.getBookFb2Content(bookId);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_XML)
                    .header(HttpHeaders.CONTENT_TYPE, "application/xml; charset=utf-8")
                    .body(fb2Content);
        } catch (Exception e) {
            log.error("Ошибка при получении FB2 контента для книги ID: " + bookId, e);
            return ResponseEntity.badRequest().body("Ошибка: " + e.getMessage());
        }
    }

    // Гость: GET /api/books/{bookId}/download — скачать файл книги
    @GetMapping("/{bookId}/download")
    public ResponseEntity<Resource> downloadBook(@PathVariable Integer bookId) {
        try {
            log.info("Запрос на скачивание книги ID: {}", bookId);
            Resource resource = bookService.getBookAsResource(bookId);
            Path filePath = bookService.getBookFilePath(bookId);
            String filename = filePath.getFileName().toString();

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (Exception e) {
            log.error("Ошибка при скачивании книги ID: " + bookId, e);
            return ResponseEntity.badRequest().body(null);
        }
    }

    // ️ MODERATOR / ADMIN: POST /api/books — создать книгу
    @PostMapping
    public ResponseEntity<BookResponse> createBook(@Valid  @RequestBody BookCreateRequest request) {
        log.info("Создание новой книги: {}", request.title());
        BookResponse response = bookService.createBook(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    //  MODERATOR / ADMIN: PUT /api/books/{id} — обновить книгу
    @PutMapping("/{id}")
    public ResponseEntity<BookResponse> updateBook(
            @PathVariable Integer id,
            @Valid @RequestBody BookUpdateRequest request) {
        log.info("Обновление книги ID: {}", id);
        BookResponse response = bookService.updateBook(id, request);
        return ResponseEntity.ok(response);
    }

    //  MODERATOR / ADMIN: DELETE /api/books/{id} — удалить книгу
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Integer id) {
        log.info("Удаление книги ID: {}", id);
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }

    // Гость: GET /api/books/search — поиск по title, authorId, genreId (без пагинации)
    @GetMapping("/search")
    public ResponseEntity<Page<BookResponse>> searchBooks(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) List<Integer> authorIds,
            @RequestParam(required = false) List<Integer> genreIds,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) Double maxRating,
            Pageable pageable
    ) {
        log.info("Расширенный поиск: title={}, authorIds={}, genreIds={}, minRating={}, maxRating={}, pageable={}",
                title, authorIds, genreIds, minRating, maxRating, pageable);

        // Если сортировка не указана, сортировать по заголовку по умолчанию
        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(
                    pageable.getPageNumber(), 
                    pageable.getPageSize(), 
                    org.springframework.data.domain.Sort.by("title").ascending()
            );
        }

        Page<BookResponse> results = bookService.searchBooks(
                title, authorIds, genreIds, minRating, maxRating, pageable, null
        );
        return ResponseEntity.ok(results);
    }

    @GetMapping("/authors/{authorId}/books")
    public ResponseEntity<List<BookResponse>> getBooksByAuthor(@PathVariable Integer authorId) {
        log.info("Запрос всех книг автора ID: {}", authorId);
        List<BookResponse> books = bookService.getBooksByAuthorId(authorId);
        return ResponseEntity.ok(books);
    }


    @GetMapping("/{bookId}/cover")
    public ResponseEntity<Resource> getBookCover(@PathVariable Integer bookId) {
        try {
            Resource resource = bookService.getBookCover(bookId);

            if (resource == null || !resource.exists()) {
                log.warn("Обложка не найдена для книги ID: {}", bookId);
                return ResponseEntity.notFound().build();
            }

            String contentType = Files.probeContentType(resource.getFile().toPath());
            if (contentType == null) {
                contentType = MediaType.IMAGE_JPEG_VALUE;
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable") // Кэш на год
                    .body(resource);

        } catch (Exception e) {
            log.error("Ошибка получения обложки для книги ID: {}", bookId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}