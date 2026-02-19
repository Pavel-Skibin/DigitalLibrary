package org.nahap.userservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Сущность для избранных книг пользователя (лайки)
 */
@Entity
@Table(
        name = "user_book_favorites",
        indexes = {
                @Index(name = "idx_favorites_user_id", columnList = "user_id"),
                @Index(name = "idx_favorites_book_id", columnList = "book_id"),
                @Index(name = "idx_favorites_added", columnList = "added_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_favorites_user_book", 
                                columnNames = {"user_id", "book_id"})
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBookFavorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "book_id", nullable = false)
    private Integer bookId;

    @Column(name = "added_at")
    private LocalDateTime addedAt;

    @PrePersist
    protected void onCreate() {
        if (addedAt == null) {
            addedAt = LocalDateTime.now();
        }
    }
}
