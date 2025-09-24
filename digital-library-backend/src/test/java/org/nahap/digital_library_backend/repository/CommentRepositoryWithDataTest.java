package org.nahap.digital_library_backend.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.nahap.digital_library_backend.entity.Comment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@Tag("db")
@DisplayName("CommentRepository Tests with Real Data")
@Sql(scripts = "/sql/complete-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class CommentRepositoryWithDataTest extends BaseRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;

    @Test
    @DisplayName("Should find active comments for book ID 1 ('Зулали')")
    void shouldFindActiveCommentsByBookId() {
        List<Comment> comments = commentRepository.findActiveByBookId(1);
        assertThat(comments).hasSize(3);

        assertThat(comments).extracting(Comment::getText).containsExactlyInAnyOrder(
                "Невероятно трогательная история! Абгарян умеет найти красоту в простых вещах. Зулали стала для меня настоящим открытием армянской культуры.",
                "Читала и плакала от смеха и грусти одновременно. Такие живые персонажи, что кажется, знаешь их лично. Рекомендую всем!",
                "Хорошая книга, но местами затянуто. Хотя атмосфера армянской деревни передана великолепно."
        );
    }

    @Test
    @DisplayName("Should paginate active comments for book ID 1")
    void shouldPaginateActiveComments() {
        var page = commentRepository.findActiveByBookId(1, PageRequest.of(0, 2));
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should find active comments by user ID 1 (LiamSmith)")
    void shouldFindActiveCommentsByUserId() {
        List<Comment> comments = commentRepository.findActiveByUserId(1);
        assertThat(comments).hasSize(4); // ← Было 3, стало 4
        assertThat(comments).extracting(c -> c.getBook().getTitle())
                .contains(
                        "Зулали",
                        "Понаехавшая",
                        "Пелагия и белый бульдог",
                        "Сокровища Валькирии. Стоящий у Солнца"
                );
    }

    @Test
    @DisplayName("Should find active comment by user and book (LiamSmith + Зулали)")
    void shouldFindActiveCommentByUserAndBook() {
        Optional<Comment> comment = commentRepository.findActiveByUserAndBook(1, 1);
        assertThat(comment).isPresent();
        assertThat(comment.get().getText()).startsWith("Невероятно трогательная история");
    }

    @Test
    @DisplayName("Should NOT find deleted comment (user 3, book 1)")
    void shouldNotFindDeletedComment() {
        Optional<Comment> comment = commentRepository.findActiveByUserAndBook(3, 1);
        assertThat(comment).isEmpty(); // потому что deleted_at NOT NULL
    }

    @Test
    @DisplayName("Admin should see all comments for book 1 (including deleted)")
    void adminShouldSeeAllCommentsIncludingDeleted() {
        List<Comment> all = commentRepository.findAllByBookId(1);
        assertThat(all).hasSize(4); // включая id=20 (удалённый)
        assertThat(all).extracting(Comment::getId)
                .contains(1, 2, 3, 20);
    }
}