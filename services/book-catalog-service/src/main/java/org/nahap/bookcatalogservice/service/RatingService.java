package org.nahap.bookcatalogservice.service;

import org.nahap.bookcatalogservice.dto.request.RatingCreateRequest;
import org.nahap.bookcatalogservice.dto.response.RatingResponse;

import java.util.List;

public interface RatingService {
    RatingResponse setOrUpdateRating(RatingCreateRequest request, Integer currentUserId);

    List<RatingResponse> getRatingsByUser(Integer userId);

    Double getAverageRatingForBook(Integer bookId);

    Long getTotalRatingsForBook(Integer bookId);

    List<RatingResponse> getRatingsByBook(Integer bookId);
}