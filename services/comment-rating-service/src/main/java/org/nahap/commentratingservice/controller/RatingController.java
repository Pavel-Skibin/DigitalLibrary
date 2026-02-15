package org.nahap.commentratingservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nahap.commentratingservice.dto.request.RatingCreateRequest;
import org.nahap.commentratingservice.dto.response.RatingResponse;
import org.nahap.commentratingservice.security.JwtUserPrincipal;
import org.nahap.commentratingservice.service.RatingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    // USER: POST /api/ratings - set/update rating
    @PostMapping
    public ResponseEntity<RatingResponse> setRating(
            @Valid @RequestBody RatingCreateRequest request,
            @AuthenticationPrincipal JwtUserPrincipal currentUser) {
        Integer currentUserId = currentUser.getUserId();
        log.info("User {} rating book {}", currentUserId, request.bookId());
        RatingResponse response = ratingService.setOrUpdateRating(request, currentUserId);
        return ResponseEntity.ok(response);
    }

    // USER: GET /api/ratings/me - my ratings
    @GetMapping("/me")
    public ResponseEntity<List<RatingResponse>> getMyRatings(
            @AuthenticationPrincipal JwtUserPrincipal currentUser) {
        Integer currentUserId = currentUser.getUserId();
        log.info("Request for ratings by user ID: {}", currentUserId);
        List<RatingResponse> ratings = ratingService.getRatingsByUser(currentUserId);
        return ResponseEntity.ok(ratings);
    }

    // GET /api/ratings/books/{bookId} - all ratings for book
    @GetMapping("/books/{bookId}")
    public ResponseEntity<List<RatingResponse>> getRatingsForBook(@PathVariable Integer bookId) {
        log.info("Request for all ratings for book ID: {}", bookId);
        List<RatingResponse> ratings = ratingService.getRatingsByBook(bookId);
        return ResponseEntity.ok(ratings);
    }
}
