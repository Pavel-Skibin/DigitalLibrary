package org.nahap.userservice.dto.request;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Запрос на завершение сессии чтения
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EndReadingSessionRequest {

    @Min(value = 0, message = "Duration must be non-negative")
    private Integer durationSeconds;

    private String lastPosition; // CFI для EPUB
}
