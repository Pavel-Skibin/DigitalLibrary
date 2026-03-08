package org.nahap.userservice.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Запрос на начало сессии чтения
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StartReadingSessionRequest {

    @NotNull(message = "Book ID is required")
    @Positive(message = "Book ID must be positive")
    private Integer bookId;
}
