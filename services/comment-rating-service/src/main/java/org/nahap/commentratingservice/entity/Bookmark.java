package org.nahap.commentratingservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Закладка в книге (позиция чтения)
 * В микросервисной архитектуре хранит только ID пользователя и книги
 */
@Entity
@Table(
        name = "bookmarks",
        indexes = {
                @Index(name = "idx_bookmark_user_id", columnList = "user_id"),
                @Index(name = "idx_bookmark_book_id", columnList = "book_id"),
                @Index(name = "idx_bookmark_deleted_at", columnList = "deleted_at")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Bookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "book_id", nullable = false)
    private Integer bookId;

    @Column(name = "position", nullable = false)
    private Double position;

    @Column(name = "name", length = 255)
    private String name;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
