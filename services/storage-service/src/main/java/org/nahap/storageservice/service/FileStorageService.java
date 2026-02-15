package org.nahap.storageservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nahap.storageservice.config.StorageProperties;
import org.nahap.storageservice.dto.FileUploadResponse;
import org.nahap.storageservice.exception.FileStorageException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final StorageProperties storageProperties;

    /**
     * Upload book file (FB2/EPUB/PDF)
     * Путь в БД будет вида: /Акутагава. Ворота Расёмон.fb2
     */
    public FileUploadResponse uploadBook(MultipartFile file, String authorLastName, String bookTitle) {
        if (file.isEmpty()) {
            throw new FileStorageException("File is empty");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null) {
            throw new FileStorageException("Invalid file name");
        }

        try {
            // Build file name
            String newFileName = buildBookFileName(authorLastName, bookTitle, originalName);

            // Storage path
            Path storagePath = Paths.get(storageProperties.getBooksPath())
                    .normalize()
                    .toAbsolutePath();

            // Create directory if not exists
            if (!Files.exists(storagePath)) {
                Files.createDirectories(storagePath);
            }

            // Full path to file
            Path targetPath = storagePath.resolve(newFileName).normalize();

            // Security check (path traversal)
            if (!targetPath.startsWith(storagePath)) {
                throw new SecurityException("Attempt to write outside storage directory");
            }

            // Save file
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            log.info("Book saved: {}", targetPath);

            // Relative path for DB (starts with /)
            String relativePath = "/" + newFileName;

            return new FileUploadResponse(
                    relativePath,
                    originalName,
                    newFileName,
                    file.getSize()
            );

        } catch (IOException e) {
            log.error("Error saving book file: {}", originalName, e);
            throw new FileStorageException("Could not save file: " + e.getMessage(), e);
        }
    }

    /**
     * Upload cover image
     * Путь в БД будет вида: covers/1b2e1278-010c-4644-bbcf-5175763451ae.jpg
     */
    public FileUploadResponse uploadCover(MultipartFile file) {
        if (file.isEmpty()) {
            throw new FileStorageException("File is empty");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null) {
            throw new FileStorageException("Invalid file name");
        }

        String extension = getFileExtension(originalName);
        if (!isImageFile(extension)) {
            throw new FileStorageException("Only image files are allowed");
        }

        try {
            // Generate unique filename
            String newFileName = UUID.randomUUID() + extension;

            // Storage path
            Path storagePath = Paths.get(storageProperties.getCoversPath())
                    .normalize()
                    .toAbsolutePath();

            if (!Files.exists(storagePath)) {
                Files.createDirectories(storagePath);
            }

            Path targetPath = storagePath.resolve(newFileName).normalize();

            if (!targetPath.startsWith(storagePath)) {
                throw new SecurityException("Attempt to write outside storage directory");
            }

            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            log.info("Cover saved: {}", targetPath);

            // Relative path for DB: covers/uuid.jpg
            String relativePath = "covers/" + newFileName;

            return new FileUploadResponse(
                    relativePath,
                    originalName,
                    newFileName,
                    file.getSize()
            );

        } catch (IOException e) {
            log.error("Error saving cover file: {}", originalName, e);
            throw new FileStorageException("Could not save file: " + e.getMessage(), e);
        }
    }

    /**
     * Load file as Resource
     * filePath format: /Акутагава. Ворота Расёмон.fb2 или covers/uuid.jpg
     */
    public Resource loadFile(String filePath) {
        try {
            Path baseStoragePath;
            String cleanPath;

            if (filePath.startsWith("covers/")) {
                // Cover file
                baseStoragePath = Paths.get(storageProperties.getCoversPath());
                cleanPath = filePath.substring(7); // Remove "covers/"
            } else {
                // Book file
                baseStoragePath = Paths.get(storageProperties.getBooksPath());
                cleanPath = filePath.startsWith("/") ? filePath.substring(1) : filePath;
            }

            Path file = baseStoragePath.resolve(cleanPath).normalize();

            // Security check
            if (!file.startsWith(baseStoragePath)) {
                throw new SecurityException("Attempt to access file outside storage directory");
            }

            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new FileStorageException("File not found: " + filePath);
            }

        } catch (MalformedURLException e) {
            throw new FileStorageException("File not found: " + filePath, e);
        }
    }

    /**
     * Delete file
     */
    public void deleteFile(String filePath) {
        try {
            Path baseStoragePath;
            String cleanPath;

            if (filePath.startsWith("covers/")) {
                baseStoragePath = Paths.get(storageProperties.getCoversPath());
                cleanPath = filePath.substring(7);
            } else {
                baseStoragePath = Paths.get(storageProperties.getBooksPath());
                cleanPath = filePath.startsWith("/") ? filePath.substring(1) : filePath;
            }

            Path file = baseStoragePath.resolve(cleanPath).normalize();

            if (!file.startsWith(baseStoragePath)) {
                throw new SecurityException("Attempt to delete file outside storage directory");
            }

            Files.deleteIfExists(file);
            log.info("File deleted: {}", file);

        } catch (IOException e) {
            throw new FileStorageException("Could not delete file: " + filePath, e);
        }
    }

    /**
     * Build book filename: ФамилияАвтора.НазваниеКниги.fb2
     */
    private String buildBookFileName(String authorLastName, String bookTitle, String originalName) {
        if (StringUtils.hasText(authorLastName) && StringUtils.hasText(bookTitle)) {
            String extension = getFileExtension(originalName);
            String sanitizedAuthor = sanitizeFileName(authorLastName);
            String sanitizedTitle = sanitizeFileName(bookTitle);
            return sanitizedAuthor + "." + sanitizedTitle + extension;
        }
        return sanitizeFileName(originalName);
    }

    /**
     * Sanitize filename (remove invalid characters)
     */
    private String sanitizeFileName(String name) {
        return name
                .replaceAll("[\\\\/:*?\"<>|]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot) : "";
    }

    private boolean isImageFile(String extension) {
        return extension.matches("\\.(jpg|jpeg|png|gif|webp)");
    }
}
