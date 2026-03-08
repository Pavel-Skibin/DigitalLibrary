package org.nahap.bookcatalogservice.dto.response;

public record TagResponse(
        Integer id,
        String name,
        String category
) {}
