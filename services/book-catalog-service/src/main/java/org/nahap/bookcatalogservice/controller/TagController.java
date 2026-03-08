package org.nahap.bookcatalogservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nahap.bookcatalogservice.dto.response.TagResponse;
import org.nahap.bookcatalogservice.entity.Tag;
import org.nahap.bookcatalogservice.repository.TagRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagRepository tagRepository;

    /** Гость: GET /api/tags — все теги (сортировка по алфавиту) */
    @GetMapping
    public ResponseEntity<List<TagResponse>> getAllTags() {
        List<TagResponse> tags = tagRepository.findAll(Sort.by("name"))
                .stream()
                .map(t -> new TagResponse(t.getId(), t.getName(), t.getCategory()))
                .toList();
        return ResponseEntity.ok(tags);
    }

    /** ADMIN: POST /api/tags — создать тег (или вернуть существующий с таким именем) */
    @PostMapping
    public ResponseEntity<TagResponse> createTag(
            @RequestParam String name,
            @RequestParam(required = false) String category) {
        String trimmedName = name.trim();
        // find-or-create: если тег с таким именем уже есть — возвращаем его
        return tagRepository.findByNameIgnoreCase(trimmedName)
                .map(existing -> {
                    log.debug("Тег уже существует: {} (id={})", existing.getName(), existing.getId());
                    return ResponseEntity.ok(
                            new TagResponse(existing.getId(), existing.getName(), existing.getCategory()));
                })
                .orElseGet(() -> {
                    Tag tag = new Tag();
                    tag.setName(trimmedName);
                    tag.setCategory(category != null ? category.trim() : null);
                    Tag saved = tagRepository.save(tag);
                    log.info("Создан тег: {} [{}]", saved.getName(), saved.getCategory());
                    return ResponseEntity.status(201)
                            .body(new TagResponse(saved.getId(), saved.getName(), saved.getCategory()));
                });
    }

    /** ADMIN: DELETE /api/tags/{id} — удалить тег */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTag(@PathVariable Integer id) {
        tagRepository.deleteById(id);
        log.info("Удалён тег ID: {}", id);
        return ResponseEntity.noContent().build();
    }
}
