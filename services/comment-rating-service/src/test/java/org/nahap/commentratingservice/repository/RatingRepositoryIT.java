package org.nahap.commentratingservice.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nahap.commentratingservice.entity.Rating;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DataJpaTest
@ActiveProfiles("test")
class RatingRepositoryIT {

    @Autowired
    private RatingRepository ratingRepository;

    private Rating r1; // userId=1, bookId=10, value=5
    private Rating r2; // userId=2, bookId=10, value=3
    private Rating r3; // userId=1, bookId=20, value=4

    @BeforeEach
    void setUp() {
        r1 = ratingRepository.save(new Rating(null, 1, 10, 5));
        r2 = ratingRepository.save(new Rating(null, 2, 10, 3));
        r3 = ratingRepository.save(new Rating(null, 1, 20, 4));
    }

    @Test
    void findByBookId_returnsAllRatingsForBook() {
        List<Rating> result = ratingRepository.findByBookId(10);
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Rating::getValue)
                .containsExactlyInAnyOrder(5, 3);
    }

    @Test
    void findByBookId_noRatings_returnsEmpty() {
        List<Rating> result = ratingRepository.findByBookId(999);
        assertThat(result).isEmpty();
    }

    @Test
    void findByUserId_returnsAllRatingsForUser() {
        List<Rating> result = ratingRepository.findByUserId(1);
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Rating::getBookId)
                .containsExactlyInAnyOrder(10, 20);
    }

    @Test
    void findByUserAndBook_existingRating_returnsRating() {
        Optional<Rating> result = ratingRepository.findByUserAndBook(1, 10);
        assertThat(result).isPresent();
        assertThat(result.get().getValue()).isEqualTo(5);
    }

    @Test
    void findByUserAndBook_nonExisting_returnsEmpty() {
        Optional<Rating> result = ratingRepository.findByUserAndBook(99, 99);
        assertThat(result).isEmpty();
    }

    @Test
    void findAverageRatingByBookId_multipleRatings_calculatesCorrectly() {
        // bookId=10: ratings 5 + 3 → average = 4.0
        Double avg = ratingRepository.findAverageRatingByBookId(10);
        assertThat(avg).isCloseTo(4.0, within(0.01));
    }

    @Test
    void findAverageRatingByBookId_noRatings_returnsNull() {
        Double avg = ratingRepository.findAverageRatingByBookId(999);
        assertThat(avg).isNull();
    }

    @Test
    void countRatingsByBookId_returnsCorrectCount() {
        Long count = ratingRepository.countRatingsByBookId(10);
        assertThat(count).isEqualTo(2L);
    }

    @Test
    void countTotalRatings_returnsSumOfAllRatings() {
        Long total = ratingRepository.countTotalRatings();
        assertThat(total).isEqualTo(3L);
    }

    @Test
    void getGlobalAverageRating_allRatings_calculatesCorrectly() {
        // (5 + 3 + 4) / 3 = 4.0
        Double globalAvg = ratingRepository.getGlobalAverageRating();
        assertThat(globalAvg).isCloseTo(4.0, within(0.01));
    }

    @Test
    void getRatingDistributionByBook_returnsGroupedByValue() {
        List<Object[]> distribution = ratingRepository.getRatingDistributionByBook(10);
        // bookId=10 has value=3 (count=1) and value=5 (count=1)
        assertThat(distribution).hasSize(2);
    }
}
