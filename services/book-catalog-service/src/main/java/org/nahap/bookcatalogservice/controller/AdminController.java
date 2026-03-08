package org.nahap.bookcatalogservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nahap.bookcatalogservice.service.GlobalSettingsService;
import org.nahap.bookcatalogservice.service.RatingCacheSyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Контроллер для административных операций
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final RatingCacheSyncService ratingCacheSyncService;
    private final GlobalSettingsService globalSettingsService;

    /**
     * Синхронизирует кэш рейтингов из Comment Rating Service
     * Этот endpoint нужно вызвать один раз после миграции на микросервисы
     * чтобы заполнить кэш существующими рейтингами
     */
    /** Возвращает статус глобального переключателя читалки (без аутентификации) */
    @GetMapping("/reading-status")
    public ResponseEntity<Map<String, Object>> getReadingStatus() {
        return ResponseEntity.ok(Map.of("readingEnabled", globalSettingsService.isReadingEnabled()));
    }

    /** Переключает глобальный доступ к чтению книг (только ADMIN) */
    @PostMapping("/reading-toggle")
    public ResponseEntity<Map<String, Object>> toggleReading() {
        boolean newValue = globalSettingsService.toggleReading();
        log.info("Admin: reading toggled -> {}", newValue);
        return ResponseEntity.ok(Map.of(
                "readingEnabled", newValue,
                "message", newValue ? "Чтение книг включено" : "Чтение книг отключено"
        ));
    }

    @PostMapping("/sync-ratings-cache")
    public ResponseEntity<Map<String, Object>> synchronizeRatingsCache() {
        log.info("Admin: Manual rating cache sync requested");
        
        try {
            int updatedCount = ratingCacheSyncService.synchronizeAllRatings();
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Rating cache synchronized successfully",
                    "updatedBooksCount", updatedCount
            ));
        } catch (Exception e) {
            log.error("Admin: Rating cache sync failed", e);
            
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Rating cache synchronization failed: " + e.getMessage()
            ));
        }
    }
}
