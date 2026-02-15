package org.nahap.bookcatalogservice.service;

import org.nahap.bookcatalogservice.dto.request.CommentCreateRequest;
import org.nahap.bookcatalogservice.dto.response.CommentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CommentService {

    CommentResponse createComment(CommentCreateRequest request, Integer currentUserId);

    CommentResponse updateComment(Integer commentId, String newText, Integer currentUserId);

    void softDeleteComment(Integer commentId, Integer currentUserId, boolean isAdminOrModerator);

    Page<CommentResponse> getActiveCommentsByBook(Integer bookId, Pageable pageable);

    List<CommentResponse> getAllCommentsByBookForModerator(Integer bookId);

    void restoreComment(Integer commentId, Integer currentUserId, boolean isAdminOrModerator);
}