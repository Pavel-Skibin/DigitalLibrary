// src/main/java/org/nahap/digital_library_backend/controller/GenreController.java
package org.nahap.bookcatalogservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nahap.bookcatalogservice.dto.response.GenreResponse;
import org.nahap.bookcatalogservice.service.GenreService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/genres")
@RequiredArgsConstructor
public class GenreController {

    private final GenreService genreService;

    //  Гость: GET /api/genres — все жанры
    @GetMapping
    public ResponseEntity<List<GenreResponse>> getAllGenres() {
        log.info("Запрос всех жанров");
        List<GenreResponse> genres = genreService.getAllGenres();
        return ResponseEntity.ok(genres);
    }

    //  Гость: GET /api/genres/{id} — детали жанра
    @GetMapping("/{id}")
    public ResponseEntity<GenreResponse> getGenreById(@PathVariable Integer id) {
        log.info("Запрос жанра ID: {}", id);
        GenreResponse response = genreService.getGenreById(id);
        return ResponseEntity.ok(response);
    }

    // MODERATOR/ADMIN: POST /api/genres — создать жанр
    @PostMapping
    public ResponseEntity<GenreResponse> createGenre(@RequestParam String name) {
        log.info("Создание жанра: {}", name);
        GenreResponse response = genreService.createGenre(name);
        return ResponseEntity.status(201).body(response);
    }

    //  MODERATOR/ADMIN: PUT /api/genres/{id} — обновить жанр
    @PutMapping("/{id}")
    public ResponseEntity<GenreResponse> updateGenre(@PathVariable Integer id,
                                                     @RequestParam String name) {
        log.info("Обновление жанра ID: {} -> {}", id, name);
        GenreResponse response = genreService.updateGenre(id, name);
        return ResponseEntity.ok(response);
    }

    // MODERATOR/ADMIN: DELETE /api/genres/{id} — удалить жанр
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGenre(@PathVariable Integer id) {
        log.info("Удаление жанра ID: {}", id);
        genreService.deleteGenre(id);
        return ResponseEntity.noContent().build();
    }
}