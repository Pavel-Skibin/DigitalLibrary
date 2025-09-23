package org.nahap.digital_library_backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;
import org.hibernate.annotations.Check;

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
@Check(constraints = "value >= 1 AND value <= 5")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"user", "book"})
@ToString(exclude = {"user", "book"})
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(name = "value", nullable = false)
    @Min(1)
    @Max(5)
    private Integer value;
}