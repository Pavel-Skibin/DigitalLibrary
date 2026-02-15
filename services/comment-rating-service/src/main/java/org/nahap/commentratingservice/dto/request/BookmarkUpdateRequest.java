package org.nahap.commentratingservice.dto.request;

public record BookmarkUpdateRequest(
        double position,
        String name,
        String notes
) {}
