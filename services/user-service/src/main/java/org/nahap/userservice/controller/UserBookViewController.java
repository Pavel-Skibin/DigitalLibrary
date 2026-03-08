package org.nahap.userservice.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.nahap.userservice.dto.request.BookViewRequest;
import org.nahap.userservice.dto.response.BookViewResponse;
import org.nahap.userservice.security.CustomUserDetails;
import org.nahap.userservice.service.UserBookViewService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Контроллер для работы с историей просмотров книг
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
public class UserBookViewController {

    private final UserBookViewService viewService;

    public UserBookViewController(UserBookViewService viewService) {
        this.viewService = viewService;
    }

    /**
     * Получить историю просмотров пользователя
     * GET /api/users/me/history
     */
    @GetMapping("/me/history")
    public ResponseEntity<Page<BookViewResponse>> getMyHistory(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PageableDefault(size = 50) Pageable pageable
    ) {
        Integer userId = currentUser.getId();
        log.info("Getting view history for user: {}", userId);
        Page<BookViewResponse> history = viewService.getUserHistory(userId, pageable);
        return ResponseEntity.ok(history);
    }

    /**
     * Получить ID просмотренных книг
     * GET /api/users/me/history/book-ids
     */
    @GetMapping("/me/history/book-ids")
    public ResponseEntity<List<Integer>> getMyViewedBookIds(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        Integer userId = currentUser.getId();
        List<Integer> bookIds = viewService.getUserViewedBookIds(userId);
        return ResponseEntity.ok(bookIds);
    }

    /**
     * Записать просмотр книги (из фронтенда)
     * POST /api/users/me/views
     */
    @PostMapping("/me/views")
    public ResponseEntity<Void> recordMyView(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestBody Map<String, Integer> request
    ) {
        Integer userId = currentUser.getId();
        Integer bookId = request.get("bookId");
        
        log.info("📥 Пользователь {} открыл читалку для книги {}", userId, bookId);
        
        try {
            viewService.recordView(userId, bookId);
            log.info("✅ Просмотр успешно записан: user={}, book={}", userId, bookId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("❌ Ошибка записи просмотра: user={}, book={}, error={}", 
                    userId, bookId, e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Записать просмотр книги (внутренний endpoint для других сервисов)
     * POST /api/internal/views
     */
    @PostMapping("/internal/views")
    public ResponseEntity<Void> recordView(@Valid @RequestBody BookViewRequest request) {
        log.info("📥 Получен запрос на запись просмотра: user={}, book={}", request.getUserId(), request.getBookId());
        try {
            viewService.recordView(request);
            log.info("✅ Просмотр успешно записан: user={}, book={}", request.getUserId(), request.getBookId());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("❌ Ошибка записи просмотра: user={}, book={}, error={}", 
                    request.getUserId(), request.getBookId(), e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }
}
