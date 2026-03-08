package org.nahap.commentratingservice.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nahap.commentratingservice.entity.Comment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class CommentRepositoryIT {

    @Autowired
    private CommentRepository commentRepository;

    private Comment activeComment1;
    private Comment activeComment2;
    private Comment deletedComment;

    @BeforeEach
    void setUp() {
        // userId=1, bookId=10: два активных комментария
        activeComment1 = commentRepository.save(
                new Comment(null, 1, 10, "Отличная книга!", null, null));
        activeComment2 = commentRepository.save(
                new Comment(null, 2, 10, "Советую всем", null, null));
        // userId=1, bookId=10: удалённый комментарий
        deletedComment = commentRepository.save(
                new Comment(null, 1, 10, "Удалённый", null, LocalDateTime.now().minusDays(1)));
    }

    @Test
    void findActiveByBookId_returnsOnlyActiveComments() {
        Page<Comment> page = commentRepository.findActiveByBookId(10, PageRequest.of(0, 10));
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent())
                .extracting(Comment::getText)
                .containsExactlyInAnyOrder("Отличная книга!", "Советую всем");
    }

    @Test
    void findActiveByBookId_noActiveComments_returnsEmpty() {
        Page<Comment> page = commentRepository.findActiveByBookId(999, PageRequest.of(0, 10));
        assertThat(page.getContent()).isEmpty();
    }

    @Test
    void findActiveByUserId_returnsOnlyActiveForUser() {
        List<Comment> result = commentRepository.findActiveByUserId(1);
        // userId=1 имеет 1 активный (activeComment1) и 1 удалённый (deletedComment)
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getText()).isEqualTo("Отличная книга!");
    }

    @Test
    void findActiveByUserAndBook_existingActive_returnsComment() {
        Optional<Comment> result = commentRepository.findActiveByUserAndBook(1, 10);
        assertThat(result).isPresent();
        assertThat(result.get().getText()).isEqualTo("Отличная книга!");
    }

    @Test
    void findActiveByUserAndBook_onlyDeletedCommentExists_returnsEmpty() {
        // userId=5, bookId=10: единственный комментарий сразу удалён — findActive не должен его вернуть
        commentRepository.save(new Comment(null, 5, 10, "Скрытый", null, LocalDateTime.now()));
        Optional<Comment> result = commentRepository.findActiveByUserAndBook(5, 10);
        assertThat(result).isEmpty();
    }

    @Test
    void findActiveById_deletedComment_returnsEmpty() {
        Optional<Comment> result = commentRepository.findActiveById(deletedComment.getId());
        assertThat(result).isEmpty();
    }

    @Test
    void findActiveById_activeComment_returnsComment() {
        Optional<Comment> result = commentRepository.findActiveById(activeComment1.getId());
        assertThat(result).isPresent();
    }

    @Test
    void countActiveComments_excludesDeleted() {
        Long count = commentRepository.countActiveComments();
        assertThat(count).isEqualTo(2L);
    }

    @Test
    void countActiveCommentsByBookId_forBookWithComments() {
        Long count = commentRepository.countActiveCommentsByBookId(10);
        assertThat(count).isEqualTo(2L);
    }

    @Test
    void countActiveCommentsByBookId_forBookWithoutComments_returnsZero() {
        Long count = commentRepository.countActiveCommentsByBookId(999);
        assertThat(count).isEqualTo(0L);
    }

    @Test
    void findRecentComments_returnsActiveOrdered() {
        List<Comment> recent = commentRepository.findRecentComments(PageRequest.of(0, 5));
        assertThat(recent).hasSize(2);
    }
}
