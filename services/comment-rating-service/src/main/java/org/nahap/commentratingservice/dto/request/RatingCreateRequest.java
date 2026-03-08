package org.nahap.commentratingservice.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;

public record RatingCreateRequest(
        @NotNull(message = "ID книги не может быть пустым")
        Integer bookId,

        @Min(value = 1, message = "Оценка должна быть от 1 до 5")
        @Max(value = 5, message = "Оценка должна быть от 1 до 5")
        Integer value
) {}
