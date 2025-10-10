package org.nahap.digital_library_backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nahap.digital_library_backend.dto.request.RatingCreateRequest;
import org.nahap.digital_library_backend.dto.response.RatingResponse;
import org.nahap.digital_library_backend.security.CustomUserDetails;
import org.nahap.digital_library_backend.service.RatingService;
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

    //  USER: POST /api/ratings — поставить/изменить оценку
    @PostMapping
    public ResponseEntity<RatingResponse> setRating(@Valid @RequestBody RatingCreateRequest request,
                                                    @AuthenticationPrincipal CustomUserDetails currentUser) {
        Integer currentUserId = currentUser.getId();
        log.info("Пользователь {} ставит оценку книге {}", currentUserId, request.bookId());
        RatingResponse response = ratingService.setOrUpdateRating(request, currentUserId);
        return ResponseEntity.ok(response);
    }

    // USER: GET /api/ratings/me — мои оценки
    @GetMapping("/me")
    public ResponseEntity<List<RatingResponse>> getMyRatings(@AuthenticationPrincipal CustomUserDetails currentUser) {
        Integer currentUserId = currentUser.getId();
        log.info("Запрос оценок пользователя ID: {}", currentUserId);
        List<RatingResponse> ratings = ratingService.getRatingsByUser(currentUserId);
        return ResponseEntity.ok(ratings);
    }


    @GetMapping("/books/{bookId}")
    public ResponseEntity<List<RatingResponse>> getRatingsForBook(@PathVariable Integer bookId) {
        log.info("Запрос всех оценок книги ID: {} (аудит)", bookId);
        List<RatingResponse> ratings = ratingService.getRatingsByBook(bookId); // нужен новый метод
        return ResponseEntity.ok(ratings);
    }
}