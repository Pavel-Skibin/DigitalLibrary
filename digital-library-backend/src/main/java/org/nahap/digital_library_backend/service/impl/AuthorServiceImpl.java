package org.nahap.digital_library_backend.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nahap.digital_library_backend.dto.mapper.AuthorMapper;
import org.nahap.digital_library_backend.dto.response.AuthorResponse;
import org.nahap.digital_library_backend.entity.Author;
import org.nahap.digital_library_backend.exception.AuthorHasBooksException;
import org.nahap.digital_library_backend.exception.AuthorNotFoundException;
import org.nahap.digital_library_backend.exception.InvalidAuthorNameException;
import org.nahap.digital_library_backend.repository.AuthorRepository;
import org.nahap.digital_library_backend.service.AuthorService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorServiceImpl implements AuthorService {

    private final AuthorRepository authorRepository;
    private final AuthorMapper authorMapper;

    @Override
    @Transactional
    public AuthorResponse createAuthor(String firstName, String lastName) {
        String validatedFirstName = validateAndNormalizeName(firstName, "Имя");
        String validatedLastName = validateAndNormalizeName(lastName, "Фамилия");

        Author author = new Author();
        author.setFirstName(validatedFirstName);
        author.setLastName(validatedLastName);

        Author saved = authorRepository.save(author);
        log.info("Создан автор: {} {}", saved.getFirstName(), saved.getLastName());
        return authorMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public AuthorResponse updateAuthor(Integer authorId, String firstName, String lastName) {
        Author author = authorRepository.findById(authorId)
                .orElseThrow(() -> new AuthorNotFoundException("Автор с ID " + authorId + " не найден"));

        boolean changed = false;

        if (firstName != null && !firstName.isBlank()) {
            String trimmedFirstName = firstName.trim();
            if (!trimmedFirstName.equals(author.getFirstName())) {
                String validatedFirstName = validateAndNormalizeName(trimmedFirstName, "Имя");
                author.setFirstName(validatedFirstName);
                changed = true;
            }
        }

        if (lastName != null && !lastName.isBlank()) {
            String trimmedLastName = lastName.trim();
            if (!trimmedLastName.equals(author.getLastName())) {
                String validatedLastName = validateAndNormalizeName(trimmedLastName, "Фамилия");
                author.setLastName(validatedLastName);
                changed = true;
            }
        }

        if (changed) {
            author = authorRepository.save(author);
            log.info("Обновлён автор ID {}: {} {}", authorId, author.getFirstName(), author.getLastName());
        }

        return authorMapper.toResponse(author);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuthorResponse> searchAuthors(String query, Pageable pageable) {
        Page<Author> authorPage;

        if (query == null || query.isBlank()) {
            authorPage = authorRepository.findAll(pageable);
        } else {
            String q = query.trim();
            authorPage = authorRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(q, q, pageable);
        }

        List<AuthorResponse> responses = authorPage.getContent().stream()
                .map(author -> new AuthorResponse(
                        author.getId(),
                        author.getFirstName(),
                        author.getLastName(),
                        author.getFirstName() + " " + author.getLastName()
                ))
                .toList();

        return new PageImpl<>(responses, pageable, authorPage.getTotalElements());
    }

    @Override
    public AuthorResponse getAuthorById(Integer id) {
        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new AuthorNotFoundException("Автор с ID " + id + " не найден"));
        return authorMapper.toResponse(author);
    }

    private String validateAndNormalizeName(String name, String fieldName) {
        if (name == null) {
            throw new InvalidAuthorNameException(fieldName + " не может быть null");
        }
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            throw new InvalidAuthorNameException(fieldName + " не может быть пустым или состоять только из пробелов");
        }
        return trimmed;
    }


    @Override
    @Transactional
    public void deleteAuthor(Integer authorId) {
        Author author = authorRepository.findById(authorId)
                .orElseThrow(() -> new AuthorNotFoundException("Автор с ID " + authorId + " не найден"));

        if (authorRepository.hasBooks(authorId)) {
            throw new AuthorHasBooksException("Невозможно удалить автора: у него есть связанные книги");
        }
        authorRepository.delete(author);
        log.info("Удалён автор ID {}: {} {}", authorId, author.getFirstName(), author.getLastName());
    }


}