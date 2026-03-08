// BookUploadServiceImpl.java
package org.nahap.bookcatalogservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nahap.bookcatalogservice.configuration.BookStorageProperties;
import org.nahap.bookcatalogservice.dto.response.FileUploadResponse;
import org.nahap.bookcatalogservice.exception.BookStorageException;
import org.nahap.bookcatalogservice.service.BookUploadService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookUploadServiceImpl implements BookUploadService {

    private final BookStorageProperties storageProperties;

    @Override
    public FileUploadResponse uploadAndRenameBook(
            MultipartFile file,
            String authorLastName,
            String bookTitle
    ) {
        if (file.isEmpty()) {
            throw new BookStorageException("Файл пустой");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase().endsWith(".fb2")) {
            throw new BookStorageException("Поддерживаются только файлы .fb2");
        }

        try {
            // Формируем имя файла
            String newFileName = buildFileName(authorLastName, bookTitle, originalName);

            // Путь сохранения
            Path storagePath = Paths.get(storageProperties.getStoragePath())
                    .normalize()
                    .toAbsolutePath();

            // Создаём директорию, если не существует
            if (!Files.exists(storagePath)) {
                Files.createDirectories(storagePath);
            }

            // Полный путь к файлу
            Path targetPath = storagePath.resolve(newFileName).normalize();

            // Проверка безопасности (path traversal)
            if (!targetPath.startsWith(storagePath)) {
                throw new SecurityException("Попытка записи за пределы директории хранилища");
            }

            // Сохраняем файл
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            log.info("Файл сохранён: {}", targetPath);

            // Относительный путь для БД (начинается с /)
            String relativePath = "/" + newFileName;

            return new FileUploadResponse(
                    relativePath,
                    originalName,
                    newFileName,
                    file.getSize()
            );

        } catch (IOException e) {
            log.error("Ошибка сохранения файла: {}", originalName, e);
            throw new BookStorageException("Не удалось сохранить файл: " + e.getMessage());
        }
    }

    /**
     * Формирует имя файла: ФамилияАвтора.НазваниеКниги.fb2
     * Если данные отсутствуют, использует оригинальное имя
     */
    private String buildFileName(String authorLastName, String bookTitle, String originalName) {
        if (StringUtils.hasText(authorLastName) && StringUtils.hasText(bookTitle)) {
            String sanitizedAuthor = sanitizeFileName(authorLastName);
            String sanitizedTitle = sanitizeFileName(bookTitle);
            return sanitizedAuthor + "." + sanitizedTitle + ".fb2";
        }

        // Если данных нет, используем оригинальное имя
        return sanitizeFileName(originalName);
    }

    /**
     * Очищает строку от недопустимых символов для имени файла
     */
    private String sanitizeFileName(String name) {
        return name
                .replaceAll("[\\\\/:*?\"<>|]", "") // Убираем запрещённые символы
                .replaceAll("\\s+", "_")            // Пробелы -> подчёркивания
                .trim();
    }
}