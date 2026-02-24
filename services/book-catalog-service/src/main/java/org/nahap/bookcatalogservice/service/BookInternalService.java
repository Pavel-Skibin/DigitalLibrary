package org.nahap.bookcatalogservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.nahap.bookcatalogservice.entity.Book;
import org.nahap.bookcatalogservice.repository.BookRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for Internal API operations
 * Handles proper eager loading of book relations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookInternalService {

    private final BookRepository bookRepository;

    /**
     * Get books by IDs with all relations eagerly loaded
     * Uses step-by-step loading to avoid MultipleBagFetchException
     * 
     * @param bookIds list of book IDs
     * @return list of books with all relations loaded
     */
    @Transactional(readOnly = true)
    public List<Book> getBooksWithRelations(List<Integer> bookIds) {
        log.debug("Loading {} books with all relations", bookIds.size());
        
        // Load books with authors first
        List<Book> books = bookRepository.findAllByIdWithAuthors(bookIds);
        
        // Force initialization of other collections within transaction
        for (Book book : books) {
            // Initialize genres
            Hibernate.initialize(book.getBookGenres());
            if (book.getBookGenres() != null) {
                book.getBookGenres().forEach(bg -> 
                    Hibernate.initialize(bg.getGenre())
                );
            }
            
            // Initialize tags
            Hibernate.initialize(book.getBookTags());
            if (book.getBookTags() != null) {
                book.getBookTags().forEach(bt -> 
                    Hibernate.initialize(bt.getTag())
                );
            }
        }
        
        log.debug("Successfully loaded {} books with relations", books.size());
        return books;
    }
}
