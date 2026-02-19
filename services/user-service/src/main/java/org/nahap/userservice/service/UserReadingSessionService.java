package org.nahap.userservice.service;

import lombok.extern.slf4j.Slf4j;
import org.nahap.userservice.dto.request.EndReadingSessionRequest;
import org.nahap.userservice.dto.request.StartReadingSessionRequest;
import org.nahap.userservice.dto.response.ReadingSessionResponse;
import org.nahap.userservice.entity.UserReadingSession;
import org.nahap.userservice.repository.UserReadingSessionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Сервис для работы с сессиями чтения
 */
@Slf4j
@Service
public class UserReadingSessionService {

    private final UserReadingSessionRepository sessionRepository;
    private final UserBookViewService bookViewService;

    public UserReadingSessionService(UserReadingSessionRepository sessionRepository,
                                    UserBookViewService bookViewService) {
        this.sessionRepository = sessionRepository;
        this.bookViewService = bookViewService;
    }

    /**
     * Начать новую сессию чтения
     */
    @Transactional
    public ReadingSessionResponse startSession(Integer userId, StartReadingSessionRequest request) {
        // Проверить, нет ли уже активной сессии для этой книги
        sessionRepository.findByUserIdAndBookIdAndEndedAtIsNull(userId, request.getBookId())
                .ifPresent(existingSession -> {
                    log.warn("Active session already exists: user={}, book={}, closing it", userId, request.getBookId());
                    // Закрыть старую сессию (пользователь забыл закрыть)
                    existingSession.endSession(0);
                    sessionRepository.save(existingSession);
                });

        UserReadingSession session = UserReadingSession.builder()
                .userId(userId)
                .bookId(request.getBookId())
                .build();

        session = sessionRepository.save(session);
        log.info("Started reading session: user={}, book={}, sessionId={}", userId, request.getBookId(), session.getId());

        return mapToResponse(session);
    }

    /**
     * Обновить активную сессию чтения (heartbeat) - БЕЗ завершения
     */
    @Transactional
    public ReadingSessionResponse updateSession(Long sessionId, EndReadingSessionRequest request) {
        UserReadingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Reading session not found: " + sessionId));

        // Обновляем только duration и position, НЕ завершаем сессию
        session.setDurationSeconds(request.getDurationSeconds());
        
        if (request.getLastPosition() != null) {
            session.setLastPosition(request.getLastPosition());
        }
        
        // Обновляем флаг significant (3+ минуты)
        session.setIsSignificant(request.getDurationSeconds() != null && request.getDurationSeconds() >= 180);

        session = sessionRepository.save(session);
        log.info("Updated reading session (heartbeat): sessionId={}, duration={}, significant={}", 
                sessionId, request.getDurationSeconds(), session.getIsSignificant());

        return mapToResponse(session);
    }

    /**
     * Завершить сессию чтения
     * После завершения автоматически обновляет агрегированную статистику
     */
    @Transactional
    public ReadingSessionResponse endSession(Long sessionId, EndReadingSessionRequest request) {
        UserReadingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Reading session not found: " + sessionId));

        if (session.getEndedAt() != null) {
            log.warn("Session already ended: {}", sessionId);
            return mapToResponse(session);
        }

        session.endSession(request.getDurationSeconds());
        if (request.getLastPosition() != null) {
            session.setLastPosition(request.getLastPosition());
        }

        session = sessionRepository.save(session);
        log.info("Ended reading session: sessionId={}, duration={}, significant={}", 
                sessionId, request.getDurationSeconds(), session.getIsSignificant());

        // КЛЮЧЕВОЕ: Обновляем агрегированную статистику для системы рекомендаций
        try {
            bookViewService.updateAggregatedStats(session);
        } catch (Exception e) {
            log.error("❌ Ошибка при обновлении агрегированной статистики: {}", e.getMessage(), e);
            // Не пробрасываем исключение - статистика не критична
        }

        return mapToResponse(session);
    }

    /**
     * Получить активные сессии пользователя
     */
    @Transactional(readOnly = true)
    public List<ReadingSessionResponse> getActiveSessions(Integer userId) {
        return sessionRepository.findByUserIdAndEndedAtIsNullOrderByStartedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Получить историю сессий чтения
     */
    @Transactional(readOnly = true)
    public Page<ReadingSessionResponse> getUserReadingSessions(Integer userId, Pageable pageable) {
        Page<UserReadingSession> sessions = sessionRepository.findByUserIdOrderByStartedAtDesc(userId, pageable);
        return sessions.map(this::mapToResponse);
    }

    /**
     * Получить ID книг из значимых сессий (для рекомендаций)
     */
    @Transactional(readOnly = true)
    public List<Integer> getSignificantBookIds(Integer userId, int limit) {
        List<UserReadingSession> sessions = sessionRepository.findTopSignificantSessions(
                userId, 
                PageRequest.of(0, limit)
        );
        return sessions.stream()
                .map(UserReadingSession::getBookId)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Общее время чтения пользователя (в секундах)
     */
    @Transactional(readOnly = true)
    public Long getTotalReadingTime(Integer userId) {
        return sessionRepository.getTotalReadingTimeByUserId(userId);
    }

    /**
     * Количество завершенных сессий
     */
    @Transactional(readOnly = true)
    public Long getCompletedSessionsCount(Integer userId) {
        return sessionRepository.countByUserIdAndEndedAtIsNotNull(userId);
    }

    private ReadingSessionResponse mapToResponse(UserReadingSession session) {
        return ReadingSessionResponse.builder()
                .sessionId(session.getId())
                .bookId(session.getBookId())
                .startedAt(session.getStartedAt())
                .endedAt(session.getEndedAt())
                .durationSeconds(session.getDurationSeconds())
                .lastPosition(session.getLastPosition())
                .isSignificant(session.getIsSignificant())
                .build();
    }
}
