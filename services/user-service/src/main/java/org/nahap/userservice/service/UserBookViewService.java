package org.nahap.userservice.service;

import lombok.extern.slf4j.Slf4j;
import org.nahap.userservice.dto.request.BookViewRequest;
import org.nahap.userservice.dto.response.BookViewResponse;
import org.nahap.userservice.entity.UserBookView;
import org.nahap.userservice.entity.UserReadingSession;
import org.nahap.userservice.repository.UserBookViewRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Сервис для работы с историей просмотров книг и агрегированной статистикой
 */
@Slf4j
@Service
public class UserBookViewService {

    private final UserBookViewRepository viewRepository;

    public UserBookViewService(UserBookViewRepository viewRepository) {
        this.viewRepository = viewRepository;
    }

    /**
     * Записать просмотр книги
     * Теперь использует UPSERT: создает новую запись или обновляет существующую
     */
    @Transactional
    public UserBookView recordView(Integer userId, Integer bookId) {
        log.info("💾 Сохранение просмотра в БД: userId={}, bookId={}", userId, bookId);
        
        // Проверяем, есть ли уже запись для этой пары user-book
        UserBookView view = viewRepository.findByUserIdAndBookId(userId, bookId)
                .orElseGet(() -> {
                    log.info("🆕 Создание новой записи просмотра для user={}, book={}", userId, bookId);
                    return UserBookView.builder()
                            .userId(userId)
                            .bookId(bookId)
                            .build();
                });
        
        UserBookView saved = viewRepository.save(view);
        log.info("✅ Просмотр сохранен в БД с ID={}: user={}, book={}", saved.getId(), userId, bookId);
        return saved;
    }

    /**
     * Записать просмотр книги (из запроса)
     */
    @Transactional
    public void recordView(BookViewRequest request) {
        recordView(request.getUserId(), request.getBookId());
    }

    /**
     * Получить историю просмотров пользователя
     */
    @Transactional(readOnly = true)
    public Page<BookViewResponse> getUserHistory(Integer userId, Pageable pageable) {
        log.info("📚 Запрос истории просмотров для пользователя userId={}, pageable={}", userId, pageable);
        Page<UserBookView> views = viewRepository.findByUserIdOrderByViewedAtDesc(userId, pageable);
        log.info("📊 Найдено просмотров: {} (всего: {})", views.getNumberOfElements(), views.getTotalElements());
        return views.map(this::mapToResponse);
    }

    /**
     * Получить ID уникальных просмотренных книг
     */
    @Transactional(readOnly = true)
    public List<Integer> getUserViewedBookIds(Integer userId) {
        return viewRepository.findDistinctBookIdsByUserId(userId);
    }

    /**
     * Проверить, просматривал ли пользователь книгу
     */
    @Transactional(readOnly = true)
    public boolean hasUserViewedBook(Integer userId, Integer bookId) {
        return viewRepository.existsByUserIdAndBookId(userId, bookId);
    }

    /**
     * Количество уникальных книг, просмотренных пользователем
     */
    @Transactional(readOnly = true)
    public Long countUserViewedBooks(Integer userId) {
        return viewRepository.countDistinctBooksByUserId(userId);
    }

    /**
     * Обновить агрегированную статистику после завершения сессии чтения
     * КЛЮЧЕВОЙ МЕТОД для системы рекомендаций!
     */
    @Transactional
    public void updateAggregatedStats(UserReadingSession session) {
        if (session == null || session.getUserId() == null || session.getBookId() == null) {
            log.warn("⚠️ Cannot update aggregated stats: invalid session data");
            return;
        }

        log.info("📊 Обновление агрегированной статистики: user={}, book={}, duration={}s", 
                session.getUserId(), session.getBookId(), session.getDurationSeconds());

        // Получаем или создаем запись просмотра
        UserBookView view = viewRepository.findByUserIdAndBookId(session.getUserId(), session.getBookId())
                .orElseGet(() -> {
                    log.info("🆕 Создание новой записи для статистики: user={}, book={}", 
                            session.getUserId(), session.getBookId());
                    return UserBookView.builder()
                            .userId(session.getUserId())
                            .bookId(session.getBookId())
                            .build();
                });

        // Обновляем агрегаты из сессии
        view.updateFromSession(session);

        UserBookView saved = viewRepository.save(view);
        log.info("✅ Статистика обновлена: totalTime={}s ({} мин), sessions={}, lastRead={}", 
                saved.getTotalReadingTimeSeconds(), 
                saved.getTotalReadingTimeMinutes(),
                saved.getSessionsCount(), 
                saved.getLastReadAt());
    }

    /**
     * Получить топ книг пользователя по времени чтения
     */
    @Transactional(readOnly = true)
    public List<UserBookView> getTopBooksByReadingTime(Integer userId, int limit) {
        return viewRepository.findTopBooksByReadingTime(userId, 
                org.springframework.data.domain.PageRequest.of(0, limit));
    }

    /**
     * Получить последние прочитанные книги
     */
    @Transactional(readOnly = true)
    public List<UserBookView> getRecentlyReadBooks(Integer userId, int limit) {
        return viewRepository.findRecentlyReadBooks(userId, 
                org.springframework.data.domain.PageRequest.of(0, limit));
    }

    /**
     * Получить завершенные книги пользователя
     */
    @Transactional(readOnly = true)
    public List<UserBookView> getCompletedBooks(Integer userId) {
        return viewRepository.findByUserIdAndIsCompletedTrue(userId);
    }

    private BookViewResponse mapToResponse(UserBookView view) {
        return BookViewResponse.builder()
                .id(view.getId())
                .bookId(view.getBookId())
                .viewedAt(view.getViewedAt())
                .totalReadingTimeSeconds(view.getTotalReadingTimeSeconds())
                .sessionsCount(view.getSessionsCount())
                .lastReadAt(view.getLastReadAt())
                .lastPosition(view.getLastPosition())
                .isCompleted(view.getIsCompleted())
                .build();
    }
}
