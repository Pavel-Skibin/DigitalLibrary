
package org.nahap.digital_library_backend.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nahap.digital_library_backend.dto.mapper.GenreMapper;
import org.nahap.digital_library_backend.dto.response.GenreResponse;
import org.nahap.digital_library_backend.entity.Genre;
import org.nahap.digital_library_backend.exception.GenreAlreadyExistsException;
import org.nahap.digital_library_backend.exception.GenreNotFoundException;
import org.nahap.digital_library_backend.exception.InvalidGenreNameException;
import org.nahap.digital_library_backend.repository.GenreRepository;
import org.nahap.digital_library_backend.service.GenreService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GenreServiceImpl implements GenreService {

    private final GenreRepository genreRepository;
    private final GenreMapper genreMapper;

    @Override
    @Transactional
    public GenreResponse createGenre(String name) {
        String validatedName = validateAndNormalizeGenreName(name);

        if (genreRepository.findByName(validatedName).isPresent()) {
            throw new GenreAlreadyExistsException("Жанр с названием '" + validatedName + "' уже существует");
        }

        Genre genre = new Genre();
        genre.setName(validatedName);

        Genre saved = genreRepository.save(genre);
        log.info("Создан жанр: {}", saved.getName());
        return genreMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public GenreResponse updateGenre(Integer genreId, String name) {
        Genre genre = genreRepository.findById(genreId)
                .orElseThrow(() -> new GenreNotFoundException("Жанр с ID " + genreId + " не найден"));

        String validatedName = validateAndNormalizeGenreName(name);

        if (!validatedName.equals(genre.getName())) {
            if (genreRepository.findByName(validatedName).isPresent()) {
                throw new GenreAlreadyExistsException("Жанр с названием '" + validatedName + "' уже существует");
            }
            genre.setName(validatedName);
            genre = genreRepository.save(genre);
            log.info("Обновлён жанр ID {}: {}", genreId, validatedName);
        }

        return genreMapper.toResponse(genre);
    }

    @Override
    public List<GenreResponse> getAllGenres() {
        return genreRepository.findAll().stream()
                .map(genreMapper::toResponse)
                .toList();
    }

    @Override
    public GenreResponse getGenreById(Integer id) {
        Genre genre = genreRepository.findById(id)
                .orElseThrow(() -> new GenreNotFoundException("Жанр с ID " + id + " не найден"));
        return genreMapper.toResponse(genre);
    }

    private String validateAndNormalizeGenreName(String name) {
        if (name == null) {
            throw new InvalidGenreNameException("Название жанра не может быть null");
        }
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            throw new InvalidGenreNameException("Название жанра не может быть пустым или состоять только из пробелов");
        }
        return trimmed;
    }

    @Override
    @Transactional
    public void deleteGenre(Integer genreId) {
        Genre genre = genreRepository.findById(genreId)
                .orElseThrow(() -> new GenreNotFoundException("Жанр с ID " + genreId + " не найден"));

        if (genreRepository.hasBooks(genreId)) {
            throw new IllegalStateException("Невозможно удалить жанр: у него есть связанные книги");
        }

        genreRepository.delete(genre);
        log.info("Удалён жанр ID {}: {}", genreId, genre.getName());
    }
}