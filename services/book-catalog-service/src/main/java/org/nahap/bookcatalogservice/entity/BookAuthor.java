package org.nahap.bookcatalogservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "book_authors")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"book", "author"})
@ToString(exclude = {"book", "author"})
public class BookAuthor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private Author author;
}