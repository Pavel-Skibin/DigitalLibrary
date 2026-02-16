package org.nahap.commentratingservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nahap.commentratingservice.client.book.InternalBookApiApi;
import org.nahap.commentratingservice.client.book.model.RatingCacheUpdateRequest;
import org.nahap.commentratingservice.client.user.InternalUserApiApi;
import org.nahap.commentratingservice.dto.mapper.RatingMapper;
import org.nahap.commentratingservice.dto.request.RatingCreateRequest;
import org.nahap.commentratingservice.dto.response.RatingResponse;
import org.nahap.commentratingservice.entity.Rating;
import org.nahap.commentratingservice.repository.RatingRepository;
import org.nahap.commentratingservice.service.RatingService;
import org.nahap.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RatingServiceImpl implements RatingService {

    private final RatingRepository ratingRepository;
    private final RatingMapper ratingMapper;
    private final InternalUserApiApi userServiceClient;
    private final InternalBookApiApi bookServiceClient;

    @Override
    @Transactional
    public RatingResponse setOrUpdateRating(RatingCreateRequest request, Integer currentUserId) {
        // Validate user exists
        var userValidation = userServiceClient.validateUser(currentUserId);
        if (!userValidation.getExists()) {
            throw new ResourceNotFoundException("User not found: " + currentUserId);
        }

        // Validate book exists
        var bookValidation = bookServiceClient.validateBook(request.bookId());
        if (!bookValidation.getExists()) {
            throw new ResourceNotFoundException("Book not found: " + request.bookId());
        }

        ratingRepository.findByUserAndBook(currentUserId, request.bookId())
                .ifPresentOrElse(
                        existingRating -> {
                            existingRating.setValue(request.value());
                            ratingRepository.save(existingRating);
                            log.info("Updated rating for book {} by user {}: {}", request.bookId(), currentUserId, request.value());
                        },
                        () -> {
                            Rating newRating = new Rating();
                            newRating.setUserId(currentUserId);
                            newRating.setBookId(request.bookId());
                            newRating.setValue(request.value());
                            ratingRepository.save(newRating);
                            log.info("Created rating for book {} by user {}: {}", request.bookId(), currentUserId, request.value());
                        }
                );

        Rating currentRating = ratingRepository.findByUserAndBook(currentUserId, request.bookId())
                .orElseThrow(() -> new IllegalStateException("Rating not found after save"));
        
        // Update rating cache in Book Catalog Service
        updateBookRatingCache(request.bookId());
        
        return ratingMapper.toResponse(currentRating);
    }

    /**
     * Updates cached rating data in Book Catalog Service
     * @param bookId ID of the book to update cache for
     */
    private void updateBookRatingCache(Integer bookId) {
        try {
            BigDecimal avgRating = getAverageRatingForBook(bookId);
            Long ratingsCount = getTotalRatingsForBook(bookId);
            
            RatingCacheUpdateRequest cacheUpdate = new RatingCacheUpdateRequest();
            cacheUpdate.setAverageRating(avgRating);
            cacheUpdate.setRatingsCount(ratingsCount.intValue());
            
            bookServiceClient.updateRatingCache(bookId, cacheUpdate);
            log.info("Updated rating cache in Book Catalog for book {}: avg={}, count={}", 
                    bookId, avgRating, ratingsCount);
        } catch (Exception e) {
            log.error("Failed to update rating cache for book {}: {}", bookId, e.getMessage(), e);
            // Don't throw - rating was saved successfully, cache update is non-critical
        }
    }

    @Override
    public List<RatingResponse> getRatingsByUser(Integer userId) {
        return ratingRepository.findByUserId(userId).stream()
                .map(ratingMapper::toResponse)
                .toList();
    }

    @Override
    public List<RatingResponse> getRatingsByBook(Integer bookId) {
        return ratingRepository.findByBookId(bookId).stream()
                .map(ratingMapper::toResponse)
                .toList();
    }

    @Override
    public BigDecimal getAverageRatingForBook(Integer bookId) {
        Double avg = ratingRepository.findAverageRatingByBookId(bookId);
        if (avg == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public Long getTotalRatingsForBook(Integer bookId) {
        return ratingRepository.countRatingsByBookId(bookId);
    }
}
