package org.nahap.userservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Сущность для истории просмотров книг пользователями
 * С версии 3.0: содержит агрегированную статистику чтения для системы рекомендаций
 */
@Entity
@Table(
        name = "user_book_views",
        indexes = {
                @Index(name = "idx_views_user_id", columnList = "user_id"),
                @Index(name = "idx_views_book_id", columnList = "book_id"),
                @Index(name = "idx_views_user_book", columnList = "user_id, book_id"),
                @Index(name = "idx_views_timestamp", columnList = "viewed_at"),
                @Index(name = "idx_views_total_reading_time", columnList = "total_reading_time_seconds"),
                @Index(name = "idx_views_last_read_at", columnList = "last_read_at"),
                @Index(name = "idx_views_is_completed", columnList = "user_id, is_completed")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_views_user_book", columnNames = {"user_id", "book_id"})
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBookView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "book_id", nullable = false)
    private Integer bookId;

    /**
     * Первый просмотр книги (открытие читалки)
     */
    @Column(name = "viewed_at", nullable = false)
    private LocalDateTime viewedAt;

    /**
     * Общее время чтения книги в секундах (сумма всех сессий)
     */
    @Column(name = "total_reading_time_seconds")
    @Builder.Default
    private Integer totalReadingTimeSeconds = 0;

    /**
     * Количество сессий чтения этой книги
     */
    @Column(name = "sessions_count")
    @Builder.Default
    private Integer sessionsCount = 0;

    /**
     * Время последней сессии чтения
     */
    @Column(name = "last_read_at")
    private LocalDateTime lastReadAt;

    /**
     * Последняя позиция в книге (для продолжения чтения)
     */
    @Column(name = "last_position", length = 255)
    private String lastPosition;

    /**
     * Флаг завершения чтения книги
     */
    @Column(name = "is_completed")
    @Builder.Default
    private Boolean isCompleted = false;

    @PrePersist
    protected void onCreate() {
        if (viewedAt == null) {
            viewedAt = LocalDateTime.now();
        }
        if (totalReadingTimeSeconds == null) {
            totalReadingTimeSeconds = 0;
        }
        if (sessionsCount == null) {
            sessionsCount = 0;
        }
        if (isCompleted == null) {
            isCompleted = false;
        }
    }

    /**
     * Обновить агрегированную статистику после завершения сессии чтения
     */
    public void updateFromSession(UserReadingSession session) {
        if (session.getDurationSeconds() != null && session.getDurationSeconds() > 0) {
            this.totalReadingTimeSeconds += session.getDurationSeconds();
            this.sessionsCount += 1;
        }
        
        if (session.getEndedAt() != null) {
            this.lastReadAt = session.getEndedAt();
        } else if (session.getStartedAt() != null) {
            this.lastReadAt = session.getStartedAt();
        }
        
        if (session.getLastPosition() != null) {
            this.lastPosition = session.getLastPosition();
        }
    }

    /**
     * Получить средное время сессии в секундах
     */
    public Integer getAverageSessionDuration() {
        if (sessionsCount == null || sessionsCount == 0) {
            return 0;
        }
        return totalReadingTimeSeconds / sessionsCount;
    }

    /**
     * Получить общее время чтения в минутах
     */
    public Integer getTotalReadingTimeMinutes() {
        if (totalReadingTimeSeconds == null) {
            return 0;
        }
        return totalReadingTimeSeconds / 60;
    }
}
