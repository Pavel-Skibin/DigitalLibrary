package org.nahap.digital_library_backend.service;

import org.nahap.digital_library_backend.dto.request.RatingCreateRequest;
import org.nahap.digital_library_backend.dto.response.RatingResponse;

import java.util.List;

public interface RatingService {
    RatingResponse setOrUpdateRating(RatingCreateRequest request, Integer currentUserId);

    List<RatingResponse> getRatingsByUser(Integer userId);

    Double getAverageRatingForBook(Integer bookId);

    Long getTotalRatingsForBook(Integer bookId);

    List<RatingResponse> getRatingsByBook(Integer bookId);
}