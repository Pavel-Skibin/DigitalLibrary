package org.nahap.bookcatalogservice.dto.response;

public record AuthorResponse(
        Integer id,
        String firstName,
        String lastName,
        String fullName
) {}