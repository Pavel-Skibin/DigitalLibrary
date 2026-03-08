package org.nahap.bookcatalogservice.service;


import org.nahap.bookcatalogservice.dto.request.BookCreateRequest;
import org.nahap.bookcatalogservice.dto.request.BookUpdateRequest;
import org.nahap.bookcatalogservice.dto.response.BookDetailResponse;
import org.nahap.bookcatalogservice.dto.response.BookResponse;
import org.springframework.core.io.Resource;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.nio.file.Path;
import java.util.List;

public interface BookService {
    String getBookFb2Content(Integer bookId) throws Exception;

    Path getBookFilePath(Integer bookId) throws Exception;

    Resource getBookAsResource(Integer bookId) throws Exception;

    BookResponse createBook(BookCreateRequest request);

    BookResponse updateBook(Integer bookId, BookUpdateRequest request);

    void deleteBook(Integer bookId); // физическое удаление

    BookDetailResponse getBookDetails(Integer bookId);

    Page<BookResponse> getAllBooks(Pageable pageable);

    Page<BookResponse> searchBooks(
            String title,
            List<Integer> authorIds,
            List<Integer> genreIds,
            Double minRating,
            Double maxRating,
            Pageable pageable,
            String sort
    );

    List<BookResponse> getBooksByAuthorId(Integer authorId);

    Resource getBookCover(Integer bookId);


}