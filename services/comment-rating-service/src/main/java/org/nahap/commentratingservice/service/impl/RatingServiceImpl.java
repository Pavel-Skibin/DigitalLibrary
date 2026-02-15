package org.nahap.commentratingservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nahap.commentratingservice.dto.mapper.RatingMapper;
import org.nahap.commentratingservice.dto.request.RatingCreateRequest;
import org.nahap.commentratingservice.dto.response.RatingResponse;
import org.nahap.commentratingservice.entity.Rating;
import org.nahap.commentratingservice.repository.RatingRepository;
import org.nahap.commentratingservice.service.RatingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RatingServiceImpl implements RatingService {

    private final RatingRepository ratingRepository;
    private final RatingMapper ratingMapper;

    @Override
    @Transactional
    public RatingResponse setOrUpdateRating(RatingCreateRequest request, Integer currentUserId) {
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
        return ratingMapper.toResponse(currentRating);
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
    public Double getAverageRatingForBook(Integer bookId) {
        Double avg = ratingRepository.findAverageRatingByBookId(bookId);
        return avg == null ? 0.0 : Math.round(avg * 10.0) / 10.0;
    }

    @Override
    public Long getTotalRatingsForBook(Integer bookId) {
        return ratingRepository.countRatingsByBookId(bookId);
    }
}
