package org.nahap.userservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Сущность для сессий чтения книг
 */
@Entity
@Table(
        name = "user_reading_sessions",
        indexes = {
                @Index(name = "idx_sessions_user_id", columnList = "user_id"),
                @Index(name = "idx_sessions_book_id", columnList = "book_id"),
                @Index(name = "idx_sessions_user_book", columnList = "user_id, book_id"),
                @Index(name = "idx_sessions_started", columnList = "started_at")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserReadingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "book_id", nullable = false)
    private Integer bookId;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "last_position", length = 255)
    private String lastPosition;

    @Column(name = "is_significant")
    @Builder.Default
    private Boolean isSignificant = false;

    @PrePersist
    protected void onCreate() {
        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }
    }

    /**
     * Завершить сессию и вычислить длительность
     */
    public void endSession(Integer duration) {
        this.endedAt = LocalDateTime.now();
        this.durationSeconds = duration;
        this.isSignificant = (duration != null && duration >= 180); // 3 минуты
    }
}
