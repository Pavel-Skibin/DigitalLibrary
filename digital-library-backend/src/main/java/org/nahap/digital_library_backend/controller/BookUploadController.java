// BookUploadController.java
package org.nahap.digital_library_backend.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nahap.digital_library_backend.dto.response.FileUploadResponse;
import org.nahap.digital_library_backend.service.BookUploadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookUploadController {

    private final BookUploadService uploadService;

    @PostMapping("/upload")
    public ResponseEntity<FileUploadResponse> uploadBook(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String authorLastName,
            @RequestParam(required = false) String bookTitle
    ) {
        log.info("Загрузка книги: {}", file.getOriginalFilename());

        FileUploadResponse response = uploadService.uploadAndRenameBook(
                file, authorLastName, bookTitle
        );

        return ResponseEntity.ok(response);
    }
}