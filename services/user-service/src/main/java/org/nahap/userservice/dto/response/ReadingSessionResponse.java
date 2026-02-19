package org.nahap.userservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Ответ с информацией о сессии чтения
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReadingSessionResponse {

    private Long sessionId;
    private Integer bookId;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Integer durationSeconds;
    private String lastPosition;
    private Boolean isSignificant;
}
