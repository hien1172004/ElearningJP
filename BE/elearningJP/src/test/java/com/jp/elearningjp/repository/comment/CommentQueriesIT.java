package com.jp.elearningjp.repository.comment;

import com.jp.elearningjp.entity.comment.Comment;
import com.jp.elearningjp.shared.enums.CommentTargetType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Query mới của module bình luận + migration V39, chạy trên Postgres thật. Cần Docker. */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CommentQueriesIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired CommentRepository commentRepository;
    @Autowired CommentReplyRepository replyRepository;
    @Autowired CommentLikeRepository likeRepository;
    @Autowired CommentReplyLikeRepository replyLikeRepository;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource dataSource;

    private long userId;
    private long commentId;

    private long insertReply(Long parentId, boolean deleted) {
        return jdbc.queryForObject("""
                INSERT INTO comment_replies (comment_id, user_id, parent_reply_id, content, deleted)
                VALUES (?, ?, ?, 'r', ?) RETURNING id""", Long.class, commentId, userId, parentId, deleted);
    }

    private boolean isDeleted(long replyId) {
        return jdbc.queryForObject("SELECT deleted FROM comment_replies WHERE id = ?", Boolean.class, replyId);
    }

    private int commentColumn(String column) {
        return jdbc.queryForObject("SELECT " + column + " FROM comments WHERE id = ?", Integer.class, commentId);
    }

    @BeforeEach
    void seed() {
        userId = jdbc.queryForObject(
                "INSERT INTO users (email, password_hash, full_name) VALUES (?, 'x', 'C') RETURNING id",
                Long.class, "comment-" + System.nanoTime() + "@test.local");
        commentId = jdbc.queryForObject("""
                INSERT INTO comments (target_type, target_id, user_id, content)
                VALUES ('VOCABULARY', 1, ?, 'gốc') RETURNING id""", Long.class, userId);
    }

    @Test
    void reaction_insertIsIdempotent_deleteReportsRows_batchLookup() {
        assertThat(likeRepository.insertIfAbsent(userId, commentId)).isEqualTo(1);
        assertThat(likeRepository.insertIfAbsent(userId, commentId)).isZero(); // bấm 2 lần: không lỗi PK
        assertThat(likeRepository.findReactedIds(userId, List.of(commentId, -1L))).containsExactly(commentId);

        assertThat(likeRepository.deleteReaction(userId, commentId)).isEqualTo(1);
        assertThat(likeRepository.deleteReaction(userId, commentId)).isZero();
    }

    @Test
    void replyReaction_queriesRunOnRealSchema() {
        long reply = insertReply(null, false);
        assertThat(replyLikeRepository.insertIfAbsent(userId, reply)).isEqualTo(1);
        assertThat(replyLikeRepository.findReactedIds(userId, List.of(reply))).containsExactly(reply);
    }

    @Test
    void softDeleteSubtree_deletesDescendantsOnly() {
        long a = insertReply(null, false);
        long b = insertReply(a, false);
        long c = insertReply(b, false);
        long sibling = insertReply(null, false);

        assertThat(replyRepository.softDeleteSubtree(b, Instant.now())).isEqualTo(2);

        assertThat(isDeleted(b)).isTrue();
        assertThat(isDeleted(c)).isTrue();
        assertThat(isDeleted(a)).isFalse();
        assertThat(isDeleted(sibling)).isFalse();
    }

    @Test
    void decrementReplyCountBy_neverBelowZero() {
        jdbc.update("UPDATE comments SET reply_count = 2 WHERE id = ?", commentId);
        commentRepository.decrementReplyCountBy(commentId, 5);
        assertThat(commentColumn("reply_count")).isZero();
    }

    @Test
    void hiddenComments_excludedFromPublicQueries() {
        jdbc.update("UPDATE comments SET status = 'HIDDEN' WHERE id = ?", commentId);

        assertThat(commentRepository.findVisibleById(commentId)).isEmpty();
        assertThat(commentRepository.findByIdAndDeletedFalse(commentId)).isPresent(); // admin vẫn xoá được
        assertThat(commentRepository.findByTarget(CommentTargetType.VOCABULARY, 1L, PageRequest.of(0, 20))
                .getContent()).extracting(Comment::getId).doesNotContain(commentId);
    }

    @Test
    void updateContent_doesNotOverwriteCounterIncrementedConcurrently() {
        Comment loaded = commentRepository.findById(commentId).orElseThrow(); // like_count = 0 trong bộ nhớ
        jdbc.update("UPDATE comments SET like_count = like_count + 3 WHERE id = ?", commentId); // request khác

        loaded.setContent("đã sửa");
        commentRepository.saveAndFlush(loaded);

        assertThat(commentColumn("like_count")).isEqualTo(3); // @DynamicUpdate: chỉ ghi content
    }

    @Test
    void migrationV39_removesOrphansAndRecomputesCounters() throws Exception {
        long deletedParent = insertReply(null, true);
        long orphan = insertReply(deletedParent, false);
        long orphanChild = insertReply(orphan, false);
        long healthy = insertReply(null, false);
        jdbc.update("INSERT INTO comment_likes (user_id, comment_id) VALUES (?, ?)", userId, commentId);
        jdbc.update("UPDATE comments SET reply_count = 9, like_count = 0, dislike_count = 4 WHERE id = ?", commentId);
        entityManager.flush();

        Connection connection = org.springframework.jdbc.datasource.DataSourceUtils.getConnection(dataSource);
        ScriptUtils.executeSqlScript(connection,
                new ClassPathResource("db/migration/V39__fix_comment_counters_and_orphan_replies.sql"));

        assertThat(isDeleted(orphan)).isTrue();
        assertThat(isDeleted(orphanChild)).isTrue();
        assertThat(isDeleted(healthy)).isFalse();
        assertThat(commentColumn("reply_count")).isEqualTo(1);
        assertThat(commentColumn("like_count")).isEqualTo(1);
        assertThat(commentColumn("dislike_count")).isZero();
    }
}
