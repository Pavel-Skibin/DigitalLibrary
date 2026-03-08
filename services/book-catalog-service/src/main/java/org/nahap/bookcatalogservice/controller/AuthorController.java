// src/main/java/org/nahap/digital_library_backend/controller/AuthorController.java
package org.nahap.bookcatalogservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nahap.bookcatalogservice.dto.response.AuthorResponse;
import org.nahap.bookcatalogservice.service.AuthorService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/authors")
@RequiredArgsConstructor
public class AuthorController {

    private final AuthorService authorService;

    // Гость: GET /api/authors — список всех авторов с пагинацией и поиском
    @GetMapping
    public ResponseEntity<Page<AuthorResponse>> getAllAuthors(
            @RequestParam(required = false) String query,
            Pageable pageable) {
        log.info("Запрос списка авторов с поиском: {}, pageable: {}", query, pageable);
        Page<AuthorResponse> authors = authorService.searchAuthors(query, pageable);
        return ResponseEntity.ok(authors);
    }

    // Гость: GET /api/authors/{id} — детали автора
    @GetMapping("/{id}")
    public ResponseEntity<AuthorResponse> getAuthorById(@PathVariable Integer id) {
        log.info("Запрос автора ID: {}", id);
        AuthorResponse response = authorService.getAuthorById(id);
        return ResponseEntity.ok(response);
    }

    //  MODERATOR/ADMIN: POST /api/authors — создать автора
    @PostMapping
    public ResponseEntity<AuthorResponse> createAuthor(
            @RequestParam String firstName,
            @RequestParam String lastName) {
        log.info("Создание автора: {} {}", firstName, lastName);
        AuthorResponse response = authorService.createAuthor(firstName, lastName);
        return ResponseEntity.status(201).body(response);
    }

    // MODERATOR/ADMIN: PUT /api/authors/{id} — обновить автора
    @PutMapping("/{id}")
    public ResponseEntity<AuthorResponse> updateAuthor(
            @PathVariable Integer id,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName) {
        log.info("Обновление автора ID: {} (firstName={}, lastName={})", id, firstName, lastName);
        AuthorResponse response = authorService.updateAuthor(id, firstName, lastName);
        return ResponseEntity.ok(response);
    }

    // MODERATOR/ADMIN: DELETE /api/authors/{id} — удалить автора
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAuthor(@PathVariable Integer id) {
        log.info("Удаление автора ID: {}", id);
        authorService.deleteAuthor(id);
        return ResponseEntity.noContent().build();
    }
}