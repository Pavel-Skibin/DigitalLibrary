package org.nahap.digital_library_backend.repository;

import org.nahap.digital_library_backend.entity.Author;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.awt.print.Pageable;
import java.util.List;

@Repository
public interface AuthorRepository extends JpaRepository<Author, Integer> {


    List<Author> findByFirstNameContainingIgnoreCase(String firstName);
    List<Author> findByLastNameContainingIgnoreCase(String lastName);
    List<Author> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
            String firstName, String lastName);


}