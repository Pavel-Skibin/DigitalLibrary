package org.nahap.storageservice.dto;

public record FileUploadResponse(
        String filePath,
        String originalName,
        String savedName,
        long size
) {}
