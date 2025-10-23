package org.nahap.digital_library_backend.service;

import org.springframework.web.multipart.MultipartFile;
import org.nahap.digital_library_backend.dto.response.FileUploadResponse;

public interface BookUploadService {
    FileUploadResponse uploadAndRenameBook(
            MultipartFile file,
            String authorLastName,
            String bookTitle
    );
}