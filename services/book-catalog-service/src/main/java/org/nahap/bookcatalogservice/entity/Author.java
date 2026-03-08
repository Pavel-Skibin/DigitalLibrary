package org.nahap.bookcatalogservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(
        name = "authors",
        indexes = {
                @Index(name = "idx_author_last_name", columnList = "last_name"),
                @Index(name = "idx_author_first_name", columnList = "first_name")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Author {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    // Связи
    @OneToMany(mappedBy = "author")
    private List<BookAuthor> bookAuthors;
}