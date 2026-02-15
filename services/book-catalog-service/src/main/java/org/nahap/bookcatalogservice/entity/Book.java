package org.nahap.bookcatalogservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(
        name = "books",
        indexes = {
                @Index(name = "idx_book_title", columnList = "title"),
                @Index(name = "idx_book_pub_year", columnList = "publication_year"),
                @Index(name = "idx_book_language", columnList = "language"),
                @Index(name = "idx_book_created", columnList = "created_at")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType. IDENTITY)
    private Integer id;

    // === БАЗОВАЯ ИНФОРМАЦИЯ ===
    @Column(name = "title", nullable = false, length = 500)
    private String title;


    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // === МЕТАДАННЫЕ ===

    @Column(name = "publication_year")
    private Integer publicationYear;  // NEW

    @Column(name = "language", length = 10, nullable = false)
    private String language;  // NEW

    @Column(name = "word_count")
    private Integer wordCount;  // NEW

    @Column(name = "publisher", length = 255)
    private String publisher;  // NEW

    // === СЕРИЯ ===
    @Column(name = "series_name", length = 255)
    private String seriesName;  // NEW

    @Column(name = "series_number")
    private Integer seriesNumber;  // NEW

    // === КАТЕГОРИЗАЦИЯ ===
    @Column(name = "age_rating", length = 5)
    private String ageRating;  // NEW:  "0+", "12+", "16+", "18+"



    // === ФАЙЛЫ ===
    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "cover_image_path")
    private String coverImagePath;


    // === ВРЕМЕННЫЕ МЕТКИ ===
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;  // NEW


    // === АНАЛИТИКА (опционально, вычисляемые поля) ===
//    @Column(name = "avg_reading_time_minutes")
//    private Integer avgReadingTimeMinutes;  // NEW
//
//    @Column(name = "view_count")
//    private Long viewCount;  // NEW:сколько раз просмотрели
//
//    @Column(name = "download_count")
//    private Long downloadCount;  // NEW: сколько раз скачали



    // === СВЯЗИ  ===
    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookAuthor> bookAuthors;

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookGenre> bookGenres;

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments;

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Rating> ratings;

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Bookmark> bookmarks;

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookTag> bookTags = new ArrayList<>();

    // === LIFECYCLE CALLBACKS ===
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (language == null) {
            language = "ru";  // По умолчанию русский
        }
    }
//
//        if (viewCount == null) {
//            viewCount = 0L;
//        }
//        if (downloadCount == null) {
//            downloadCount = 0L;
//        }
//    }


}

