package org.nahap.userservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ответ со статусом избранного
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FavoriteStatusResponse {

    private Boolean isFavorite;
}
