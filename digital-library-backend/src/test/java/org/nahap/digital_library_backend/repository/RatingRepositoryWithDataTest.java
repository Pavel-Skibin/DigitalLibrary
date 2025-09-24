package org.nahap.digital_library_backend.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.nahap.digital_library_backend.entity.Rating;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@Tag("db")
@DisplayName("RatingRepository Tests with Real Data")
@Sql(scripts = "/sql/complete-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class RatingRepositoryWithDataTest extends BaseRepositoryTest {

    @Autowired
    private RatingRepository ratingRepository;

    @Test
    @DisplayName("Should find all ratings for book ID 1 ('Зулали')")
    void shouldFindRatingsByBookId() {
        List<Rating> ratings = ratingRepository.findByBookId(1);
        assertThat(ratings).hasSize(4);
        assertThat(ratings).extracting(Rating::getValue)
                .containsExactlyInAnyOrder(5, 5, 4, 4);
    }

    @Test
    @DisplayName("Should find all ratings by user ID 1 (LiamSmith)")
    void shouldFindRatingsByUserId() {
        List<Rating> ratings = ratingRepository.findByUserId(1);
        assertThat(ratings).hasSize(5);
        assertThat(ratings).extracting(r -> r.getBook().getTitle())
                .contains(
                        "Зулали",
                        "Понаехавшая",
                        "Четвертый ледниковый период",
                        "Пелагия и белый бульдог",
                        "Сокровища Валькирии. Стоящий у Солнца"
                );
    }

    @Test
    @DisplayName("Should find rating by user and book (LiamSmith + Зулали = 5)")
    void shouldFindRatingByUserAndBook() {
        Optional<Rating> rating = ratingRepository.findByUserAndBook(1, 1);
        assertThat(rating).isPresent();
        assertThat(rating.get().getValue()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should calculate average rating for 'Зулали' (4.5 → but SQL says 4.6 → 18/4 = 4.5?)")
    void shouldCalculateAverageRating_Zulali() {
        Double avg = ratingRepository.findAverageRatingByBookId(1);
        assertThat(avg).isCloseTo(4.5, within(0.01)); // (5+5+4+4)/4 = 4.5
    }

    @Test
    @DisplayName("Should count ratings for 'Метафизика' (2 ratings)")
    void shouldCountRatings_Metaphysics() {
        Long count = ratingRepository.countRatingsByBookId(9);
        assertThat(count).isEqualTo(2L);
    }
}