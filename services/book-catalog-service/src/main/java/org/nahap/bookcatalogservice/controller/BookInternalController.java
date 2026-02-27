package org.nahap.bookcatalogservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nahap.bookcatalogservice.api.internal.InternalBookApiApi;
import org.nahap.bookcatalogservice.api.internal.model.BookResponse;
import org.nahap.bookcatalogservice.api.internal.model.RatingCacheUpdateRequest;
import org.nahap.bookcatalogservice.api.internal.model.ValidationResponse;
import org.nahap.bookcatalogservice.dto.mapper.InternalBookMapper;
import org.nahap.bookcatalogservice.entity.Book;
import org.nahap.bookcatalogservice.repository.BookRepository;
import org.nahap.bookcatalogservice.service.BookInternalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Internal API Controller for inter-service communication
 * Implements OpenAPI generated interface
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class BookInternalController implements InternalBookApiApi {

    private final BookRepository bookRepository;
    private final BookInternalService bookInternalService;
    private final InternalBookMapper internalBookMapper;

    @Override
    public ResponseEntity<BookResponse> getBookById(Integer id) {
        log.debug("Internal API: Getting book by id: {}", id);

        List<Book> books = bookInternalService.getBooksWithRelations(List.of(id));
        
        if (books.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(internalBookMapper.toResponse(books.get(0)));
    }

    @Override
    public ResponseEntity<ValidationResponse> validateBook(Integer id) {
        log.debug("Internal API: Validating book existence: {}", id);
        
        boolean exists = bookRepository.existsById(id);
        
        ValidationResponse response = new ValidationResponse(exists);
        if (!exists) {
            response.setMessage("Book not found");
        }
        
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Map<String, BookResponse>> getBooksBatch(List<Integer> bookIds) {
        log.debug("Internal API: Getting batch of books: {}", bookIds);
        
        // Use service with @Transactional to properly load all relations
        List<Book> books = bookInternalService.getBooksWithRelations(bookIds);
        
        Map<String, BookResponse> bookMap = books.stream()
                .collect(Collectors.toMap(
                        book -> String.valueOf(book.getId()),
                        internalBookMapper::toResponse
                ));
        
        return ResponseEntity.ok(bookMap);
    }

    @Override
    public ResponseEntity<List<Integer>> getAllBookIds(Integer limit) {
        log.debug("Internal API: Getting all book IDs (limit={})", limit);

        List<Integer> bookIds = bookRepository.findAll().stream()
                .map(Book::getId)
                .sorted()  // Explicit sorting by ID for predictable indexing order
                .limit(limit != null ? limit : Long.MAX_VALUE)
                .collect(Collectors.toList());
        
        log.info("Internal API: Found {} book IDs{}", 
                bookIds.size(), 
                limit != null ? " (limited to " + limit + ")" : "");
        return ResponseEntity.ok(bookIds);
    }

    @Override
    public ResponseEntity<Void> updateRatingCache(Integer id, RatingCacheUpdateRequest request) {
        log.info("Internal API: Updating rating cache for book {}: avgRating={}, count={}", 
                id, request.getAverageRating(), request.getRatingsCount());
        
        return bookRepository.findById(id)
                .map(book -> {
                    book.setAverageRating(request.getAverageRating());
                    book.setRatingsCount(request.getRatingsCount());
                    bookRepository.save(book);
                    log.debug("Rating cache updated successfully for book {}", id);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElseGet(() -> {
                    log.warn("Book not found for rating cache update: {}", id);
                    return ResponseEntity.notFound().build();
                });
    }
}
