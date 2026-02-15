package org.nahap.bookcatalogservice.entity;

import jakarta. persistence.*;
import lombok.*;

@Entity
@Table(name = "book_tags")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"book", "tag"})
@ToString(exclude = {"book", "tag"})
public class BookTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;
}