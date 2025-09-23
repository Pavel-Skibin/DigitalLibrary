package org.nahap.digital_library_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "comments",
        indexes = {
                @Index(name = "idx_comment_book_id", columnList = "book_id"),
                @Index(name = "idx_comment_user_id", columnList = "user_id"),
                @Index(name = "idx_comment_created_at", columnList = "created_at"),
                @Index(name = "idx_comment_deleted_at", columnList = "deleted_at")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"user", "book"})
@ToString(exclude = {"user", "book"})
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(name = "text", nullable = false)
    private String text;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}