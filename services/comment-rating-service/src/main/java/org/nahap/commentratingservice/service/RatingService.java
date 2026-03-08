package org.nahap.commentratingservice.service;

import org.nahap.commentratingservice.dto.request.RatingCreateRequest;
import org.nahap.commentratingservice.dto.response.RatingResponse;

import java.math.BigDecimal;
import java.util.List;

public interface RatingService {

    RatingResponse setOrUpdateRating(RatingCreateRequest request, Integer currentUserId);

    List<RatingResponse> getRatingsByUser(Integer userId);

    List<RatingResponse> getRatingsByBook(Integer bookId);

    BigDecimal getAverageRatingForBook(Integer bookId);

    Long getTotalRatingsForBook(Integer bookId);
}
