package org.nahap.userservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Ответ с информацией о просмотре книги и агрегированной статистике чтения
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookViewResponse {

    private Long id;
    private Integer bookId;
    
    /**
     * Время первого просмотра книги
     */
    private LocalDateTime viewedAt;
    
    /**
     * Общее время чтения книги в секундах (все сессии)
     */
    private Integer totalReadingTimeSeconds;
    
    /**
     * Количество сессий чтения
     */
    private Integer sessionsCount;
    
    /**
     * Время последней сессии чтения
     */
    private LocalDateTime lastReadAt;
    
    /**
     * Последняя позиция в книге
     */
    private String lastPosition;
    
    /**
     * Флаг завершения книги
     */
    private Boolean isCompleted;
    
    /**
     * Получить общее время чтения в минутах
     */
    public Integer getTotalReadingTimeMinutes() {
        return totalReadingTimeSeconds != null ? totalReadingTimeSeconds / 60 : 0;
    }
    
    /**
     * Получить среднюю длительность сессии в секундах
     */
    public Integer getAverageSessionDuration() {
        if (sessionsCount == null || sessionsCount == 0) {
            return 0;
        }
        return totalReadingTimeSeconds / sessionsCount;
    }
}
