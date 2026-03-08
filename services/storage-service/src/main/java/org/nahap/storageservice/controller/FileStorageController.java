package org.nahap.storageservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nahap.storageservice.dto.FileUploadResponse;
import org.nahap.storageservice.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/storage")
@RequiredArgsConstructor
public class FileStorageController {

    private final FileStorageService fileStorageService;

    /**
     * Upload book file (FB2/EPUB/PDF)
     * MODERATOR/ADMIN only
     */
    @PostMapping("/upload/book")
    public ResponseEntity<FileUploadResponse> uploadBook(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String authorLastName,
            @RequestParam(required = false) String bookTitle
    ) {
        log.info("Uploading book: {}", file.getOriginalFilename());
        FileUploadResponse response = fileStorageService.uploadBook(file, authorLastName, bookTitle);
        return ResponseEntity.ok(response);
    }

    /**
     * Upload cover image
     * MODERATOR/ADMIN only
     */
    @PostMapping("/upload/cover")
    public ResponseEntity<FileUploadResponse> uploadCover(
            @RequestParam("file") MultipartFile file
    ) {
        log.info("Uploading cover: {}", file.getOriginalFilename());
        FileUploadResponse response = fileStorageService.uploadCover(file);
        return ResponseEntity.ok(response);
    }

    /**
     * Download book file
     * Public endpoint
     * Path example: /Акутагава. Ворота Расёмон.fb2
     */
    @GetMapping("/books/**")
    public ResponseEntity<Resource> downloadBook(
            @RequestParam(required = false) String download,
            HttpServletRequest request
    ) {
        // Extract path from URL
        String requestURI = request.getRequestURI();
        String filePath = requestURI.substring("/api/storage/books".length());
        
        log.info("Download book request: {}", filePath);
        
        Resource resource = fileStorageService.loadFile(filePath);
        
        String contentType = determineContentType(filePath);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        
        // If download=true param, force download
        if ("true".equals(download)) {
            headers.set(HttpHeaders.CONTENT_DISPOSITION, 
                "attachment; filename=\"" + resource.getFilename() + "\"");
        } else {
            headers.set(HttpHeaders.CONTENT_DISPOSITION, 
                "inline; filename=\"" + resource.getFilename() + "\"");
        }
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
    }

    /**
     * Download cover image
     * Public endpoint
     * Path example: covers/1b2e1278-010c-4644-bbcf-5175763451ae.jpg
     */
    @GetMapping("/covers/{filename}")
    public ResponseEntity<Resource> downloadCover(@PathVariable String filename) {
        log.info("Download cover request: {}", filename);
        
        String filePath = "covers/" + filename;
        Resource resource = fileStorageService.loadFile(filePath);
        
        String contentType = determineContentType(filename);
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                    "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    /**
     * Delete file (book or cover)
     * MODERATOR/ADMIN only
     */
    @DeleteMapping("/**")
    public ResponseEntity<Void> deleteFile(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        String filePath = requestURI.substring("/api/storage".length());
        
        log.info("Delete file request: {}", filePath);
        
        fileStorageService.deleteFile(filePath);
        return ResponseEntity.noContent().build();
    }

    private String determineContentType(String filename) {
        if (filename.endsWith(".fb2")) {
            return "application/x-fictionbook+xml";
        } else if (filename.endsWith(".epub")) {
            return "application/epub+zip";
        } else if (filename.endsWith(".pdf")) {
            return "application/pdf";
        } else if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (filename.endsWith(".png")) {
            return "image/png";
        } else if (filename.endsWith(".gif")) {
            return "image/gif";
        } else if (filename.endsWith(".webp")) {
            return "image/webp";
        }
        return "application/octet-stream";
    }
}
