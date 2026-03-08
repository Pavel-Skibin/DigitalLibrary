package org.nahap.commentratingservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

/**
 * Рейтинг книги от пользователя (1-5 звезд)
 * В микросервисной архитектуре хранит только ID пользователя и книги
 */
@Entity
@Table(
        name = "ratings",
        indexes = {
                @Index(name = "idx_rating_book_id", columnList = "book_id"),
                @Index(name = "idx_rating_user_id", columnList = "user_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_rating_user_book", columnNames = {"user_id", "book_id"})
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "book_id", nullable = false)
    private Integer bookId;

    @Column(name = "rating_value", nullable = false)
    @Min(1)
    @Max(5)
    private Integer value;
}
