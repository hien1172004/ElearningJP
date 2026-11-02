package com.jp.elearningjp.repository.gamification;

import com.jp.elearningjp.repository.comment.CommentReplyRepository;
import com.jp.elearningjp.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Query JSONB / đệ quy mới của module thông báo, chạy trên Postgres thật. Cần Docker. */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class NotificationQueriesIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired NotificationRepository notificationRepository;
    @Autowired UserRepository userRepository;
    @Autowired CommentReplyRepository replyRepository;
    @Autowired JdbcTemplate jdbc;

    private long userId;

    private long newUser(String status) {
        return jdbc.queryForObject(
                "INSERT INTO users (email, password_hash, full_name, status) VALUES (?, 'x', 'N', ?) RETURNING id",
                Long.class, "notif-" + System.nanoTime() + "@test.local", status);
    }

    private long notify(String type, String dataJson) {
        return jdbc.queryForObject("""
                INSERT INTO notifications (user_id, title, body, type, data_json)
                VALUES (?, 't', 'b', ?, CAST(? AS jsonb)) RETURNING id""", Long.class, userId, type, dataJson);
    }

    private boolean isDeleted(long notificationId) {
        return jdbc.queryForObject("SELECT deleted FROM notifications WHERE id = ?", Boolean.class, notificationId);
    }

    @BeforeEach
    void seed() {
        userId = newUser("ACTIVE");
    }

    @Test
    void existsCommunityLike_matchesSameActorAndTargetOnly() {
        notify("COMMUNITY_LIKE", "{\"commentId\": 10, \"actorUserId\": 2}");
        notify("COMMUNITY_LIKE", "{\"commentId\": 10, \"replyId\": 77, \"actorUserId\": 2}");

        assertThat(notificationRepository.existsCommunityLike(userId, 2L, 10L, -1L)).isTrue();   // like comment
        assertThat(notificationRepository.existsCommunityLike(userId, 2L, 10L, 77L)).isTrue();   // like reply
        assertThat(notificationRepository.existsCommunityLike(userId, 3L, 10L, -1L)).isFalse();  // người khác
        assertThat(notificationRepository.existsCommunityLike(userId, 2L, 11L, -1L)).isFalse();  // comment khác
        assertThat(notificationRepository.existsCommunityLike(userId, 2L, 10L, 78L)).isFalse();  // reply khác
    }

    @Test
    void softDelete_byCommentAndByReplies_onlyCommunityNotificationsOfThatTarget() {
        long ofComment = notify("COMMUNITY_REPLY", "{\"commentId\": 10, \"replyId\": 77}");
        long ofReply = notify("COMMUNITY_LIKE", "{\"commentId\": 20, \"replyId\": 88}");
        long otherReply = notify("COMMUNITY_LIKE", "{\"commentId\": 20, \"replyId\": 89}");
        long reminder = notify("SRS_DUE", "{\"commentId\": 10}");

        assertThat(notificationRepository.softDeleteByCommentId(10L, Instant.now())).isEqualTo(1);
        assertThat(notificationRepository.softDeleteByReplyIds(List.of("88"), Instant.now())).isEqualTo(1);

        assertThat(isDeleted(ofComment)).isTrue();
        assertThat(isDeleted(ofReply)).isTrue();
        assertThat(isDeleted(otherReply)).isFalse();
        assertThat(isDeleted(reminder)).isFalse(); // không đụng loại thông báo khác
    }

    @Test
    void findByUserId_sameTimestamp_stableOrderAcrossPages() {
        // Job nhắc nhở có thể tạo nhiều thông báo cùng thời điểm: id phân định thứ tự,
        // nên cuộn trang trong dropdown không lặp hay sót mục.
        long first = notify("SRS_DUE", "{}");
        long second = notify("STREAK_WARNING", "{}");
        long third = notify("COMMUNITY_LIKE", "{}");
        jdbc.update("UPDATE notifications SET created_at = TIMESTAMPTZ '2026-11-02 08:00:00+07' WHERE user_id = ?", userId);

        List<Long> page1 = notificationRepository.findByUserId(userId, org.springframework.data.domain.PageRequest.of(0, 2))
                .map(n -> n.getId()).getContent();
        List<Long> page2 = notificationRepository.findByUserId(userId, org.springframework.data.domain.PageRequest.of(1, 2))
                .map(n -> n.getId()).getContent();

        assertThat(page1).containsExactly(third, second);
        assertThat(page2).containsExactly(first);
    }

    @Test
    void findIdsNotEligibleForNotifications_bannedInactiveAndDeleted() {
        long banned = newUser("BANNED");
        long inactive = newUser("INACTIVE");
        long deleted = newUser("ACTIVE");
        jdbc.update("UPDATE users SET deleted = TRUE WHERE id = ?", deleted);

        assertThat(userRepository.findIdsNotEligibleForNotifications())
                .contains(banned, inactive, deleted)
                .doesNotContain(userId);
    }

    @Test
    void findActiveSubtreeIds_returnsReplyAndLiveDescendants() {
        long comment = jdbc.queryForObject("""
                INSERT INTO comments (target_type, target_id, user_id, content)
                VALUES ('VOCABULARY', 1, ?, 'c') RETURNING id""", Long.class, userId);
        long a = insertReply(comment, null, false);
        long b = insertReply(comment, a, false);
        long deadChild = insertReply(comment, a, true);
        long grandchild = insertReply(comment, b, false);

        assertThat(replyRepository.findActiveSubtreeIds(a))
                .containsExactlyInAnyOrder(a, b, grandchild)
                .doesNotContain(deadChild);
    }

    private long insertReply(long commentId, Long parentId, boolean deleted) {
        return jdbc.queryForObject("""
                INSERT INTO comment_replies (comment_id, user_id, parent_reply_id, content, deleted)
                VALUES (?, ?, ?, 'r', ?) RETURNING id""", Long.class, commentId, userId, parentId, deleted);
    }
}
