package org.nahap.digital_library_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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
@EqualsAndHashCode(exclude = {"user", "book"})
@ToString(exclude = {"user", "book"})
public class Bookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(name = "position", nullable = false)
    private Double position;

    @Column(name = "name")
    private String name;

    @Column(name = "notes")
    private String notes;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}