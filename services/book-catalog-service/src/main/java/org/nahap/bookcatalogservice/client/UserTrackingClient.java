package org.nahap.bookcatalogservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Клиент для отправки событий tracking в User Service
 */
@Slf4j
@Component
public class UserTrackingClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${services.user.url:http://localhost:8081}")
    private String userServiceUrl;

    /**
     * Асинхронно записать просмотр книги
     */
    @Async
    public void recordBookView(Integer userId, Integer bookId) {
        if (userId == null || bookId == null) {
            log.warn("Cannot record view: userId or bookId is null (userId={}, bookId={})", userId, bookId);
            return;
        }

        try {
            String url = userServiceUrl + "/api/internal/views";
            Map<String, Integer> request = Map.of(
                    "userId", userId,
                    "bookId", bookId
            );

            log.info("📊 Recording book view: userId={}, bookId={}, url={}", userId, bookId, url);
            restTemplate.postForEntity(url, request, Void.class);
            log.info("✅ View recorded successfully: user={}, book={}", userId, bookId);
        } catch (Exception e) {
            // Не падаем если User Service недоступен
            log.error("❌ Failed to record view: user={}, book={}, error={}", 
                    userId, bookId, e.getMessage(), e);
        }
    }
}
