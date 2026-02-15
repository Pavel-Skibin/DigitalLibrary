package org.nahap.commentratingservice.service;

import org.nahap.commentratingservice.dto.request.RatingCreateRequest;
import org.nahap.commentratingservice.dto.response.RatingResponse;

import java.util.List;

public interface RatingService {

    RatingResponse setOrUpdateRating(RatingCreateRequest request, Integer currentUserId);

    List<RatingResponse> getRatingsByUser(Integer userId);

    List<RatingResponse> getRatingsByBook(Integer bookId);

    Double getAverageRatingForBook(Integer bookId);

    Long getTotalRatingsForBook(Integer bookId);
}
