package org.nahap.bookcatalogservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(
        name = "genres",
        indexes = {
                @Index(name = "idx_genre_name", columnList = "name")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Genre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    // Связи
    @OneToMany(mappedBy = "genre")
    private List<BookGenre> bookGenres;
}