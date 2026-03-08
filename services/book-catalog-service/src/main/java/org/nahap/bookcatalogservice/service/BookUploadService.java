package org.nahap.bookcatalogservice.service;

import org.springframework.web.multipart.MultipartFile;
import org.nahap.bookcatalogservice.dto.response.FileUploadResponse;

public interface BookUploadService {
    FileUploadResponse uploadAndRenameBook(
            MultipartFile file,
            String authorLastName,
            String bookTitle
    );
}