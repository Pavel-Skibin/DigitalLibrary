package org.nahap.commentratingservice.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nahap.commentratingservice.client.book.InternalBookApiApi;
import org.nahap.commentratingservice.client.user.InternalUserApiApi;
import org.nahap.commentratingservice.dto.mapper.RatingMapper;
import org.nahap.commentratingservice.dto.request.RatingCreateRequest;
import org.nahap.commentratingservice.dto.response.RatingResponse;
import org.nahap.commentratingservice.entity.Rating;
import org.nahap.commentratingservice.repository.RatingRepository;
import org.nahap.common.exception.ResourceNotFoundException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RatingServiceImplTest {

    @Mock private RatingRepository ratingRepository;
    @Mock private RatingMapper ratingMapper;
    @Mock private InternalUserApiApi userServiceClient;
    @Mock private InternalBookApiApi bookServiceClient;

    @InjectMocks
    private RatingServiceImpl ratingService;

    private Rating testRating;
    private RatingResponse testRatingResponse;

    @BeforeEach
    void setUp() {
        testRating = new Rating();
        testRating.setId(1);
        testRating.setUserId(10);
        testRating.setBookId(5);
        testRating.setValue(4);

        testRatingResponse = new RatingResponse(1, 5, 4, "testuser");
    }

    private org.nahap.commentratingservice.client.user.model.ValidationResponse userExists() {
        var v = new org.nahap.commentratingservice.client.user.model.ValidationResponse();
        v.setExists(true);
        return v;
    }

    private org.nahap.commentratingservice.client.book.model.ValidationResponse bookExists() {
        var v = new org.nahap.commentratingservice.client.book.model.ValidationResponse();
        v.setExists(true);
        return v;
    }

    @Test
    void setOrUpdateRating_newRating_savesAndReturnsResponse() {
        RatingCreateRequest request = new RatingCreateRequest(5, 4);

        when(userServiceClient.validateUser(10)).thenReturn(userExists());
        when(bookServiceClient.validateBook(5)).thenReturn(bookExists());
        when(ratingRepository.findByUserAndBook(10, 5)).thenReturn(Optional.empty())
                .thenReturn(Optional.of(testRating));
        when(ratingRepository.save(any(Rating.class))).thenReturn(testRating);
        when(ratingMapper.toResponse(testRating)).thenReturn(testRatingResponse);
        when(ratingRepository.findAverageRatingByBookId(5)).thenReturn(4.0);
        when(ratingRepository.countRatingsByBookId(5)).thenReturn(1L);
        doNothing().when(bookServiceClient).updateRatingCache(eq(5), any());

        RatingResponse result = ratingService.setOrUpdateRating(request, 10);

        assertThat(result).isNotNull();
        assertThat(result.value()).isEqualTo(4);
        assertThat(result.bookId()).isEqualTo(5);
        verify(ratingRepository, atLeastOnce()).save(any(Rating.class));
    }

    @Test
    void setOrUpdateRating_existingRating_updatesValueAndReturnsResponse() {
        RatingCreateRequest request = new RatingCreateRequest(5, 5);

        when(userServiceClient.validateUser(10)).thenReturn(userExists());
        when(bookServiceClient.validateBook(5)).thenReturn(bookExists());
        when(ratingRepository.findByUserAndBook(10, 5))
                .thenReturn(Optional.of(testRating))
                .thenReturn(Optional.of(testRating));
        when(ratingRepository.save(any(Rating.class))).thenReturn(testRating);
        when(ratingMapper.toResponse(testRating)).thenReturn(testRatingResponse);
        when(ratingRepository.findAverageRatingByBookId(5)).thenReturn(4.5);
        when(ratingRepository.countRatingsByBookId(5)).thenReturn(2L);
        doNothing().when(bookServiceClient).updateRatingCache(eq(5), any());

        RatingResponse result = ratingService.setOrUpdateRating(request, 10);

        assertThat(result).isNotNull();
        // Убеждаемся, что значение рейтинга было обновлено до нового
        assertThat(testRating.getValue()).isEqualTo(5);
        verify(ratingRepository, atLeastOnce()).save(argThat(r -> r.getValue() == 5));
    }

    @Test
    void setOrUpdateRating_userNotFound_throwsResourceNotFoundException() {
        RatingCreateRequest request = new RatingCreateRequest(5, 4);

        var userNotExists = new org.nahap.commentratingservice.client.user.model.ValidationResponse();
        userNotExists.setExists(false);
        when(userServiceClient.validateUser(99)).thenReturn(userNotExists);

        assertThatThrownBy(() -> ratingService.setOrUpdateRating(request, 99))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(ratingRepository, never()).save(any());
    }

    @Test
    void getRatingsByBook_returnsRatingList() {
        when(ratingRepository.findByBookId(5)).thenReturn(List.of(testRating));
        when(ratingMapper.toResponse(testRating)).thenReturn(testRatingResponse);

        List<RatingResponse> result = ratingService.getRatingsByBook(5);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).bookId()).isEqualTo(5);
        assertThat(result.get(0).value()).isEqualTo(4);
    }

    @Test
    void getAverageRatingForBook_withRatings_returnsAverage() {
        when(ratingRepository.findAverageRatingByBookId(5)).thenReturn(4.25);

        BigDecimal result = ratingService.getAverageRatingForBook(5);

        assertThat(result).isEqualByComparingTo(new BigDecimal("4.25"));
    }

    @Test
    void getAverageRatingForBook_withNoRatings_returnsZero() {
        when(ratingRepository.findAverageRatingByBookId(5)).thenReturn(null);

        BigDecimal result = ratingService.getAverageRatingForBook(5);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getTotalRatingsForBook_returnsCount() {
        when(ratingRepository.countRatingsByBookId(5)).thenReturn(7L);

        Long result = ratingService.getTotalRatingsForBook(5);

        assertThat(result).isEqualTo(7L);
    }
}
