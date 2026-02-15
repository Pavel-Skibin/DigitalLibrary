package org.nahap.commentratingservice.dto.mapper;

import org.nahap.commentratingservice.entity.Comment;
import org.nahap.commentratingservice.dto.response.CommentResponse;
import org.springframework.stereotype.Component;

@Component
public class CommentMapper {

    public CommentResponse toResponse(Comment comment) {
        // TODO: Get username from User Service via REST
        String userName = "User#" + comment.getUserId();
        
        return new CommentResponse(
                comment.getId(),
                comment.getUserId(),
                userName,
                comment.getBookId(),
                comment.getText(),
                comment.getCreatedAt(),
                comment.getDeletedAt()
        );
    }
}
