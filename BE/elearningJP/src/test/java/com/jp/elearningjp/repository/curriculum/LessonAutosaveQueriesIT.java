package com.jp.elearningjp.repository.curriculum;

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

import static org.assertj.core.api.Assertions.assertThat;

/** Autosave (V63) trên Postgres thật: upsert JSONB, không đụng version bài, dọn bản cũ. Cần Docker. */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class LessonAutosaveQueriesIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired LessonAutosaveRepository repository;
    @Autowired UserLessonProgressRepository progressRepository;
    @Autowired JdbcTemplate jdbc;

    private long userId;
    private long otherUserId;
    private long lessonId;
    private long courseId;

    private long insertUser() {
        return jdbc.queryForObject("INSERT INTO users (email, password_hash, full_name) VALUES (?, 'x', 'U') RETURNING id",
                Long.class, "as-" + System.nanoTime() + "@test.local");
    }

    @BeforeEach
    void seed() {
        userId = insertUser();
        otherUserId = insertUser();
        courseId = jdbc.queryForObject(
                "INSERT INTO courses (title, slug, jlpt_level) VALUES ('C', ?, 'N5') RETURNING id",
                Long.class, "as-" + System.nanoTime());
        lessonId = jdbc.queryForObject("INSERT INTO lessons (course_id, title, order_index) VALUES (?, 'L', 1) RETURNING id",
                Long.class, courseId);
    }

    @Test
    void upsert_overwritesSameForm_perUser_withoutTouchingLesson() {
        long lessonVersion = jdbc.queryForObject("SELECT version FROM lessons WHERE id = ?", Long.class, lessonId);
        long revision = jdbc.queryForObject("SELECT draft_revision FROM lessons WHERE id = ?", Long.class, lessonId);

        repository.upsert(userId, lessonId, "vocab:new:1", "{\"meaning\":\"một\"}", Instant.parse("2026-11-11T01:00:00Z"));
        repository.upsert(userId, lessonId, "vocab:new:1", "{\"meaning\":\"hai\"}", Instant.parse("2026-11-11T02:00:00Z"));
        repository.upsert(otherUserId, lessonId, "vocab:new:1", "{\"meaning\":\"của người khác\"}", Instant.now());

        var mine = repository.findByUserIdAndLessonIdOrderByUpdatedAtDesc(userId, lessonId);
        assertThat(mine).hasSize(1);
        assertThat(mine.get(0).getContent()).contains("hai");
        assertThat(mine.get(0).getUpdatedAt()).isEqualTo(Instant.parse("2026-11-11T02:00:00Z"));
        assertThat(repository.countByUserIdAndLessonId(otherUserId, lessonId)).isEqualTo(1);
        // Không tăng version / draft_revision của bài
        assertThat(jdbc.queryForObject("SELECT version FROM lessons WHERE id = ?", Long.class, lessonId)).isEqualTo(lessonVersion);
        assertThat(jdbc.queryForObject("SELECT draft_revision FROM lessons WHERE id = ?", Long.class, lessonId)).isEqualTo(revision);
    }

    @Test
    void deleteOne_onlyThatForm_ofThatUser() {
        repository.upsert(userId, lessonId, "quiz:new", "{}", Instant.now());
        repository.upsert(userId, lessonId, "quiz:9", "{}", Instant.now());
        repository.upsert(otherUserId, lessonId, "quiz:new", "{}", Instant.now());

        assertThat(repository.deleteOne(userId, lessonId, "quiz:new")).isEqualTo(1);
        assertThat(repository.deleteOne(userId, lessonId, "quiz:new")).isZero();

        assertThat(repository.findByUserIdAndLessonIdOrderByUpdatedAtDesc(userId, lessonId))
                .extracting(a -> a.getFormKey()).containsExactly("quiz:9");
        assertThat(repository.countByUserIdAndLessonId(otherUserId, lessonId)).isEqualTo(1);
    }

    @Test
    void cleanup_removesOnlyStale() {
        repository.upsert(userId, lessonId, "section:1", "{}", Instant.parse("2026-09-01T00:00:00Z"));
        repository.upsert(userId, lessonId, "section:2", "{}", Instant.parse("2026-11-10T00:00:00Z"));

        assertThat(repository.deleteOlderThan(Instant.parse("2026-10-12T00:00:00Z"))).isEqualTo(1);
        assertThat(repository.findByUserIdAndLessonIdOrderByUpdatedAtDesc(userId, lessonId))
                .extracting(a -> a.getFormKey()).containsExactly("section:2");
    }

    @Test
    void staleDraftRecipients_byLessonAndByCourse_distinct() {
        long otherLesson = jdbc.queryForObject("INSERT INTO lessons (course_id, title, order_index) VALUES (?, 'L2', 2) RETURNING id",
                Long.class, courseId);
        repository.upsert(userId, lessonId, "quiz:new", "{}", Instant.now());
        repository.upsert(userId, lessonId, "quiz:9", "{}", Instant.now());
        repository.upsert(otherUserId, otherLesson, "section:new", "{}", Instant.now());

        assertThat(repository.findUserIdsByLessonIds(java.util.List.of(lessonId))).containsExactly(userId);
        assertThat(repository.findUserIdsByCourseId(courseId)).containsExactlyInAnyOrder(userId, otherUserId);
    }

    @Test
    void completedLearners_excludeOtherStatusesAndDeleted() {
        long learnerA = insertUser();
        long learnerB = insertUser();
        long learnerC = insertUser();
        jdbc.update("INSERT INTO user_lesson_progress (user_id, lesson_id, status) VALUES (?, ?, 'COMPLETED')", learnerA, lessonId);
        jdbc.update("INSERT INTO user_lesson_progress (user_id, lesson_id, status) VALUES (?, ?, 'IN_PROGRESS')", learnerB, lessonId);
        jdbc.update("INSERT INTO user_lesson_progress (user_id, lesson_id, status, deleted) VALUES (?, ?, 'COMPLETED', true)", learnerC, lessonId);

        assertThat(progressRepository.findUserIdsByLessonIdAndStatus(lessonId,
                com.jp.elearningjp.shared.enums.ProgressStatus.COMPLETED)).containsExactly(learnerA);
    }
}
