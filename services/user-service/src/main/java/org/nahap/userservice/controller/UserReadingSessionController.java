package org.nahap.userservice.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.nahap.userservice.dto.request.EndReadingSessionRequest;
import org.nahap.userservice.dto.request.StartReadingSessionRequest;
import org.nahap.userservice.dto.response.ReadingSessionResponse;
import org.nahap.userservice.security.CustomUserDetails;
import org.nahap.userservice.service.UserReadingSessionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Контроллер для работы с сессиями чтения
 */
@Slf4j
@RestController
@RequestMapping("/api/readings")
public class UserReadingSessionController {

    private final UserReadingSessionService sessionService;

    public UserReadingSessionController(UserReadingSessionService sessionService) {
        this.sessionService = sessionService;
    }

    /**
     * Начать сессию чтения
     * POST /api/readings/start
     */
    @PostMapping("/start")
    public ResponseEntity<ReadingSessionResponse> startSession(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody StartReadingSessionRequest request
    ) {
        Integer userId = currentUser.getId();
        log.info("Starting reading session: user={}, book={}", userId, request.getBookId());
        ReadingSessionResponse response = sessionService.startSession(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Обновить активную сессию чтения (heartbeat)
     * PUT /api/readings/{sessionId}/update
     */
    @PutMapping("/{sessionId}/update")
    public ResponseEntity<ReadingSessionResponse> updateSession(
            @PathVariable Long sessionId,
            @Valid @RequestBody EndReadingSessionRequest request
    ) {
        log.info("Updating reading session (heartbeat): sessionId={}, duration={}", sessionId, request.getDurationSeconds());
        ReadingSessionResponse response = sessionService.updateSession(sessionId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Завершить сессию чтения
     * PUT /api/readings/{sessionId}/end
     */
    @PutMapping("/{sessionId}/end")
    public ResponseEntity<ReadingSessionResponse> endSession(
            @PathVariable Long sessionId,
            @Valid @RequestBody EndReadingSessionRequest request
    ) {
        log.info("Ending reading session: sessionId={}, duration={}", sessionId, request.getDurationSeconds());
        ReadingSessionResponse response = sessionService.endSession(sessionId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Получить активные сессии чтения
     * GET /api/readings/active
     */
    @GetMapping("/active")
    public ResponseEntity<List<ReadingSessionResponse>> getActiveSessions(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        Integer userId = currentUser.getId();
        log.info("Getting active sessions for user: {}", userId);
        List<ReadingSessionResponse> sessions = sessionService.getActiveSessions(userId);
        return ResponseEntity.ok(sessions);
    }

    /**
     * Получить историю сессий чтения
     * GET /api/readings/history
     */
    @GetMapping("/history")
    public ResponseEntity<Page<ReadingSessionResponse>> getReadingHistory(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Integer userId = currentUser.getId();
        Page<ReadingSessionResponse> history = sessionService.getUserReadingSessions(userId, pageable);
        return ResponseEntity.ok(history);
    }
}
