package org.nahap.digital_library_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "books")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"bookAuthors", "bookGenres", "comments", "ratings", "bookmarks"})
@ToString(exclude = {"bookAuthors", "bookGenres", "comments", "ratings", "bookmarks"})
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    // Связи
    @OneToMany(mappedBy = "book")
    private List<BookAuthor> bookAuthors;

    @OneToMany(mappedBy = "book")
    private List<BookGenre> bookGenres;

    @OneToMany(mappedBy = "book")
    private List<Comment> comments;

    @OneToMany(mappedBy = "book")
    private List<Rating> ratings;

    @OneToMany(mappedBy = "book")
    private List<Bookmark> bookmarks;
}