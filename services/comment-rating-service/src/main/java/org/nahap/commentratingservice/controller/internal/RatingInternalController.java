package org.nahap.commentratingservice.controller.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nahap.commentratingservice.api.internal.InternalRatingApiApi;
import org.nahap.commentratingservice.api.internal.model.UserRatingResponse;
import org.nahap.commentratingservice.dto.response.RatingResponse;
import org.nahap.commentratingservice.repository.RatingRepository;
import org.nahap.commentratingservice.service.RatingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Internal API Controller for Rating statistics
 * Implements OpenAPI generated interface
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class RatingInternalController implements InternalRatingApiApi {

    private final RatingRepository ratingRepository;
    private final RatingService ratingService;

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

    @Override
    public ResponseEntity<List<UserRatingResponse>> getUserRatings(Integer userId) {
        log.debug("Internal API: Getting ratings for user: {}", userId);
        
        List<UserRatingResponse> ratings = ratingRepository.findByUserId(userId).stream()
                .map(rating -> {
                    var response = new UserRatingResponse();
                    response.setBookId(rating.getBookId());
                    response.setRatingValue(rating.getValue());
                    response.setCreatedAt(null); // Rating entity doesn't have createdAt
                    return response;
                })
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(ratings);
    }
}
