package org.nahap.bookcatalogservice.dto.request;

public record BookmarkUpdateRequest(
        double position,
        String name,
        String notes
) {}