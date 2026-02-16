package org.nahap.bookcatalogservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nahap.bookcatalogservice.entity.Book;
import org.nahap.bookcatalogservice.repository.BookRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Сервис для синхронизации кэша рейтингов из Comment Rating Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RatingCacheSyncService {

    private final BookRepository bookRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${services.comment-rating.url:http://localhost:8082}")
    private String commentRatingServiceUrl;

    /**
     * Синхронизирует рейтинги всех книг из Comment Rating Service в локальный кэш
     * @return количество обновленных книг
     */
    @Transactional
    public int synchronizeAllRatings() {
        log.info("Starting rating cache synchronization...");
        
        try {
            String url = commentRatingServiceUrl + "/api/internal/books/statistics/all";
            log.debug("Fetching statistics from: {}", url);
            
            // Получаем статистику всех книг с рейтингами
            List<Map<String, Object>> statistics = restTemplate.getForObject(url, ArrayList.class);
            
            if (statistics == null || statistics.isEmpty()) {
                log.warn("No book statistics received from Comment Rating Service");
                return 0;
            }
            
            log.info("Received statistics for {} books", statistics.size());
            int updatedCount = 0;
            
            for (Map<String, Object> stat : statistics) {
                try {
                    Integer bookId = (Integer) stat.get("bookId");
                    Object avgRatingObj = stat.get("averageRating");
                    Object ratingsCountObj = stat.get("ratingsCount");
                    
                    if (bookId == null) {
                        log.warn("Skipping statistic with null bookId");
                        continue;
                    }
                    
                    // Обрабатываем averageRating (может быть null)
                    BigDecimal averageRating = null;
                    if (avgRatingObj instanceof Number) {
                        averageRating = BigDecimal.valueOf(((Number) avgRatingObj).doubleValue());
                    }
                    
                    // Обрабатываем ratingsCount
                    Integer ratingsCount = 0;
                    if (ratingsCountObj instanceof  Number) {
                        ratingsCount = ((Number) ratingsCountObj).intValue();
                    }
                    
                    // Обновляем книгу
                    Book book = bookRepository.findById(bookId).orElse(null);
                    if (book != null) {
                        book.setAverageRating(averageRating);
                        book.setRatingsCount(ratingsCount);
                        bookRepository.saveAndFlush(book);
                        updatedCount++;
                        
                        log.debug("Updated book {}: avgRating={}, count={}", 
                                bookId, averageRating, ratingsCount);
                    } else {
                        log.warn("Book with id {} not found in catalog", bookId);
                    }
                    
                } catch (Exception e) {
                    log.error("Error processing statistic: {}", stat, e);
                }
            }
            
            log.info("Rating cache synchronization completed. Updated {} books", updatedCount);
            return updatedCount;
            
        } catch (Exception e) {
            log.error("Failed to synchronize rating cache", e);
            throw new RuntimeException("Rating cache synchronization failed: " + e.getMessage(), e);
        }
    }
}
