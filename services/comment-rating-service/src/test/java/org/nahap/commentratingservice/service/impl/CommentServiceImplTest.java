package org.nahap.commentratingservice.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nahap.commentratingservice.client.book.InternalBookApiApi;
import org.nahap.commentratingservice.client.user.InternalUserApiApi;
import org.nahap.commentratingservice.dto.mapper.CommentMapper;
import org.nahap.commentratingservice.dto.request.CommentCreateRequest;
import org.nahap.commentratingservice.dto.response.CommentResponse;
import org.nahap.commentratingservice.entity.Comment;
import org.nahap.commentratingservice.repository.CommentRepository;
import org.nahap.common.exception.BadRequestException;
import org.nahap.common.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock private CommentRepository commentRepository;
    @Mock private CommentMapper commentMapper;
    @Mock private InternalUserApiApi userServiceClient;
    @Mock private InternalBookApiApi bookServiceClient;

    @InjectMocks
    private CommentServiceImpl commentService;

    private Comment testComment;
    private CommentResponse testCommentResponse;

    @BeforeEach
    void setUp() {
        testComment = new Comment();
        testComment.setId(1);
        testComment.setUserId(10);
        testComment.setBookId(5);
        testComment.setText("Отличная книга!");
        testComment.setCreatedAt(LocalDateTime.now());

        testCommentResponse = new CommentResponse(
                1, 10, "testuser", 5, "Отличная книга!",
                LocalDateTime.now(), null
        );
    }

    private org.nahap.commentratingservice.client.user.model.ValidationResponse userExists() {
        var v = new org.nahap.commentratingservice.client.user.model.ValidationResponse();
        v.setExists(true);
        return v;
    }

    private org.nahap.commentratingservice.client.book.model.ValidationResponse bookExists() {
        var v = new org.nahap.commentratingservice.client.book.model.ValidationResponse();
        v.setExists(true);
        return v;
    }

    private org.nahap.commentratingservice.client.user.model.ValidationResponse userNotExists() {
        var v = new org.nahap.commentratingservice.client.user.model.ValidationResponse();
        v.setExists(false);
        return v;
    }

    @Test
    void createComment_valid_returnsCommentResponse() {
        CommentCreateRequest request = new CommentCreateRequest(5, "Отличная книга!");

        when(userServiceClient.validateUser(10)).thenReturn(userExists());
        when(bookServiceClient.validateBook(5)).thenReturn(bookExists());
        when(commentRepository.findActiveByUserAndBook(10, 5)).thenReturn(Optional.empty());
        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);
        when(commentMapper.toResponse(testComment)).thenReturn(testCommentResponse);

        CommentResponse result = commentService.createComment(request, 10);

        assertThat(result).isNotNull();
        assertThat(result.text()).isEqualTo("Отличная книга!");
        assertThat(result.userId()).isEqualTo(10);
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void createComment_userNotFound_throwsResourceNotFoundException() {
        CommentCreateRequest request = new CommentCreateRequest(5, "Текст");

        when(userServiceClient.validateUser(99)).thenReturn(userNotExists());

        assertThatThrownBy(() -> commentService.createComment(request, 99))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(commentRepository, never()).save(any());
    }

    @Test
    void createComment_duplicateComment_throwsBadRequestException() {
        CommentCreateRequest request = new CommentCreateRequest(5, "Снова комментарий");

        when(userServiceClient.validateUser(10)).thenReturn(userExists());
        when(bookServiceClient.validateBook(5)).thenReturn(bookExists());
        when(commentRepository.findActiveByUserAndBook(10, 5)).thenReturn(Optional.of(testComment));

        assertThatThrownBy(() -> commentService.createComment(request, 10))
                .isInstanceOf(BadRequestException.class);

        verify(commentRepository, never()).save(any());
    }

    @Test
    void updateComment_byOwner_returnsUpdatedResponse() {
        CommentResponse updatedResponse = new CommentResponse(
                1, 10, "testuser", 5, "Обновлённый текст",
                testComment.getCreatedAt(), null
        );

        when(commentRepository.findActiveById(1)).thenReturn(Optional.of(testComment));
        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);
        when(commentMapper.toResponse(testComment)).thenReturn(updatedResponse);

        CommentResponse result = commentService.updateComment(1, "Обновлённый текст", 10);

        assertThat(result).isNotNull();
        assertThat(result.text()).isEqualTo("Обновлённый текст");
    }

    @Test
    void updateComment_byNonOwner_throwsAccessDeniedException() {
        when(commentRepository.findActiveById(1)).thenReturn(Optional.of(testComment));

        assertThatThrownBy(() -> commentService.updateComment(1, "Чужой текст", 999))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateComment_notFound_throwsResourceNotFoundException() {
        when(commentRepository.findActiveById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.updateComment(99, "Текст", 10))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getActiveCommentsByBook_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Comment> commentPage = new PageImpl<>(List.of(testComment));

        when(commentRepository.findActiveByBookId(5, pageable)).thenReturn(commentPage);
        when(commentMapper.toResponse(testComment)).thenReturn(testCommentResponse);

        Page<CommentResponse> result = commentService.getActiveCommentsByBook(5, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void softDeleteComment_notFound_throwsResourceNotFoundException() {
        when(commentRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.softDeleteComment(99, 10, false))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void softDeleteComment_byOwner_setsDeletedAt() {
        when(commentRepository.findById(1)).thenReturn(Optional.of(testComment));
        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);

        commentService.softDeleteComment(1, 10, false);

        assertThat(testComment.getDeletedAt()).isNotNull();
        verify(commentRepository).save(testComment);
    }
}
