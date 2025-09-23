package org.nahap.digital_library_backend.dto.request;

public record BookmarkUpdateRequest(
        double position,
        String name,
        String notes
) {}