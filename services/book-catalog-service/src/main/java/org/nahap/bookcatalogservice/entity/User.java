package org.nahap.bookcatalogservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_user_deleted_at", columnList = "deleted_at"),
                @Index(name = "idx_user_role_id", columnList = "role_id")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"comments", "ratings", "bookmarks"})
@ToString(exclude = {"comments", "ratings", "bookmarks"})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private UserRole role;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // Связи
    @OneToMany(mappedBy = "user")
    private List<Comment> comments;

    @OneToMany(mappedBy = "user")
    private List<Rating> ratings;

    @OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE)
    private List<Bookmark> bookmarks;
}