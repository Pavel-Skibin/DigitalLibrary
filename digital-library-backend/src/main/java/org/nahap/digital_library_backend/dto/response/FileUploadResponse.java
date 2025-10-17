package org.nahap.digital_library_backend.dto.response;

public record FileUploadResponse(
        String filePath,
        String originalName,
        String savedName,
        long size
) {}