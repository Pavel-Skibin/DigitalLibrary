package org.nahap.commentratingservice.controller.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nahap.commentratingservice.api.internal.InternalRatingApiApi;
import org.nahap.commentratingservice.repository.RatingRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal API Controller for Rating statistics
 * Implements OpenAPI generated interface
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class RatingInternalController implements InternalRatingApiApi {

    private final RatingRepository ratingRepository;

    @Override
    public ResponseEntity<Double> getAverageRating(Integer bookId) {
        log.debug("Internal API: Getting average rating for book: {}", bookId);
        
        Double average = ratingRepository.findAverageRatingByBookId(bookId);
        
        return ResponseEntity.ok(average);
    }

    @Override
    public ResponseEntity<Long> getRatingsCount(Integer bookId) {
        log.debug("Internal API: Getting ratings count for book: {}", bookId);
        
        Long count = ratingRepository.countRatingsByBookId(bookId);
        
        return ResponseEntity.ok(count);
    }
}
