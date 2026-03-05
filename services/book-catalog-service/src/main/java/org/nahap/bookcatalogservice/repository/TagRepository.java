package org.nahap.bookcatalogservice.repository;

import org.nahap.bookcatalogservice.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Integer> {
    Optional<Tag> findByNameIgnoreCase(String name);
    List<Tag> findByIdIn(List<Integer> ids);
}
