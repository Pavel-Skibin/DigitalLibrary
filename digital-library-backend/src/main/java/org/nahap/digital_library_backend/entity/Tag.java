package org.nahap.digital_library_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java. util.List;

@Entity
@Table(
        name = "tags",
        indexes = {
                @Index(name = "idx_tag_name", columnList = "name")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;  // "философская фантастика"

    @Column(name = "category", length = 50)
    private String category;  // "тема", "настроение", "стиль", "элемент сюжета"

    @Column(name = "is_predefined")
    private Boolean isPredefined = false;  // Из базового словаря или добавлен LLM

    @Column(name = "usage_count")
    private Integer usageCount = 0;  // Сколько раз использован (для статистики)

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Связи
    @OneToMany(mappedBy = "tag")
    private List<BookTag> bookTags;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (usageCount == null) {
            usageCount = 0;
        }
        if (isPredefined == null) {
            isPredefined = false;
        }
    }
}