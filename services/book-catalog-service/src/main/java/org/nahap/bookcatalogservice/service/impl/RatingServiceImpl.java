package org.nahap.bookcatalogservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nahap.bookcatalogservice.dto.mapper.RatingMapper;
import org.nahap.bookcatalogservice.dto.request.RatingCreateRequest;
import org.nahap.bookcatalogservice.dto.response.RatingResponse;
import org.nahap.bookcatalogservice.entity.Book;
import org.nahap.bookcatalogservice.entity.Rating;
import org.nahap.bookcatalogservice.entity.User;
import org.nahap.bookcatalogservice.exception.BookNotFoundException;
import org.nahap.bookcatalogservice.exception.UserNotFoundException;
import org.nahap.bookcatalogservice.repository.BookRepository;
import org.nahap.bookcatalogservice.repository.RatingRepository;
import org.nahap.bookcatalogservice.repository.UserRepository;
import org.nahap.bookcatalogservice.service.RatingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RatingServiceImpl implements RatingService {

    private final RatingRepository ratingRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final RatingMapper ratingMapper;

    @Override
    @Transactional
    public RatingResponse setOrUpdateRating(RatingCreateRequest request, Integer currentUserId) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));
        Book book = bookRepository.findById(request.bookId())
                .orElseThrow(() -> new BookNotFoundException("Книга не найдена"));

        ratingRepository.findByUserAndBook(currentUserId, request.bookId())
                .ifPresentOrElse(
                        existingRating -> {
                            existingRating.setValue(request.value());
                            ratingRepository.save(existingRating);
                            log.info("Обновлена оценка книги {} пользователем {}: {}", request.bookId(), currentUserId, request.value());
                        },
                        () -> {
                            Rating newRating = new Rating();
                            newRating.setUser(user);
                            newRating.setBook(book);
                            newRating.setValue(request.value());
                            ratingRepository.save(newRating);
                            log.info("Создана оценка книги {} пользователем {}: {}", request.bookId(), currentUserId, request.value());
                        }
                );

        Rating currentRating = ratingRepository.findByUserAndBook(currentUserId, request.bookId())
                .orElseThrow(() -> new IllegalStateException("Оценка не найдена после сохранения"));
        return ratingMapper.toResponse(currentRating);
    }

    @Override
    public List<RatingResponse> getRatingsByUser(Integer userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("Пользователь не найден");
        }
        return ratingRepository.findByUserId(userId).stream()
                .map(ratingMapper::toResponse)
                .toList();
    }

    @Override
    public Double getAverageRatingForBook(Integer bookId) {
        if (!bookRepository.existsById(bookId)) {
            throw new BookNotFoundException("Книга не найдена");
        }
        Double avg = ratingRepository.findAverageRatingByBookId(bookId);
        return avg == null ? 0.0 : Math.round(avg * 10.0) / 10.0; // округление до 1 знака
    }

    @Override
    public Long getTotalRatingsForBook(Integer bookId) {
        if (!bookRepository.existsById(bookId)) {
            throw new BookNotFoundException("Книга не найдена");
        }
        return ratingRepository.countRatingsByBookId(bookId);
    }

    @Override
    public List<RatingResponse> getRatingsByBook(Integer bookId) {
        if (!bookRepository.existsById(bookId)) {
            throw new BookNotFoundException("Книга не найдена");
        }
        return ratingRepository.findByBookId(bookId).stream()
                .map(ratingMapper::toResponse)
                .toList();
    }
}