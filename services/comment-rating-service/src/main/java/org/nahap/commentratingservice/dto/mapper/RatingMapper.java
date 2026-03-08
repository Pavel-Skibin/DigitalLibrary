package org.nahap.commentratingservice.dto.mapper;

import org.nahap.commentratingservice.entity.Rating;
import org.nahap.commentratingservice.dto.response.RatingResponse;
import org.springframework.stereotype.Component;

@Component
public class RatingMapper {

    public RatingResponse toResponse(Rating rating) {
        // TODO: Get username from User Service via REST
        String username = "User#" + rating.getUserId();
        
        return new RatingResponse(
                rating.getId(),
                rating.getBookId(),
                rating.getValue(),
                username
        );
    }
}
