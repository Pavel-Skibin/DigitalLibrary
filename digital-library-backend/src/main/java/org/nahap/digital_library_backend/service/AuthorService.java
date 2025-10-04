package org.nahap.digital_library_backend.service;

import org.nahap.digital_library_backend.dto.response.AuthorResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AuthorService {
    AuthorResponse createAuthor(String firstName, String lastName);

    AuthorResponse updateAuthor(Integer authorId, String firstName, String lastName);

    Page<AuthorResponse> searchAuthors(String query, Pageable pageable);

    AuthorResponse getAuthorById(Integer id);

    void deleteAuthor(Integer authorId);

}