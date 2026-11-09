package com.jp.elearningjp.repository.curriculum;

import com.jp.elearningjp.entity.curriculum.CoursePublishedLesson;
import com.jp.elearningjp.entity.curriculum.CoursePublishedVersion;
import com.jp.elearningjp.shared.enums.ProgressStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Snapshot xuất bản (V56) và cách tính tiến độ theo bản học viên đang học — Postgres thật. Cần Docker. */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PublishedSnapshotQueriesIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired LessonRepository lessonRepository;
    @Autowired UserLessonProgressRepository progressRepository;
    @Autowired CoursePublishedVersionRepository versionRepository;
    @Autowired CoursePublishedLessonRepository publishedLessonRepository;
    @Autowired JdbcTemplate jdbc;

    long courseId;
    long userId;
    long lesson1;
    long lesson2;

    private long insertLesson(int order) {
        return jdbc.queryForObject(
                "INSERT INTO lessons (course_id, title, order_index) VALUES (?, 'L', ?) RETURNING id",
                Long.class, courseId, order);
    }

    private void complete(long lessonId) {
        jdbc.update("INSERT INTO user_lesson_progress (user_id, lesson_id, status) VALUES (?, ?, 'COMPLETED')",
                userId, lessonId);
    }

    /** Tạo phiên bản xuất bản chứa đúng các bài đã cho (như CourseSnapshotService.publish). */
    private long publishWith(List<Long> lessonIds) {
        CoursePublishedVersion v = versionRepository.save(CoursePublishedVersion.builder()
                .courseId(courseId).versionNo(versionRepository.findMaxVersionNo(courseId) + 1)
                .content("{\"lessons\":[]}").build());
        int order = 1;
        for (Long id : lessonIds) {
            publishedLessonRepository.save(CoursePublishedLesson.builder()
                    .publishedVersionId(v.getId()).lessonId(id).orderIndex(order++).content("{\"schemaVersion\":1}").build());
        }
        versionRepository.flush();
        jdbc.update("UPDATE courses SET published_version_id = ? WHERE id = ?", v.getId(), courseId);
        lessonRepository.markPublishedLessons(courseId, lessonIds);
        return v.getId();
    }

    @BeforeEach
    void seed() {
        courseId = jdbc.queryForObject(
                "INSERT INTO courses (title, slug, jlpt_level, is_published) VALUES ('C', ?, 'N5', TRUE) RETURNING id",
                Long.class, "course-" + System.nanoTime());
        userId = jdbc.queryForObject(
                "INSERT INTO users (email, password_hash, full_name) VALUES (?, 'x', 'S') RETURNING id",
                Long.class, "snap-" + System.nanoTime() + "@test.local");
        lesson1 = insertLesson(1);
        lesson2 = insertLesson(2);
        complete(lesson1);
    }

    @Test
    void withoutSnapshot_countsActiveLessonsAsBefore() {
        assertThat(lessonRepository.countLearnerLessons(courseId)).isEqualTo(2);
        assertThat(progressRepository.calculateCourseProgressPercent(userId, courseId)).isEqualTo(50);
    }

    @Test
    void withSnapshot_newDraftLessonNotCounted_deletedDraftLessonStillCounted() {
        publishWith(List.of(lesson1, lesson2));
        long draftOnly = insertLesson(3);                                     // bài mới chưa duyệt
        jdbc.update("UPDATE lessons SET deleted = TRUE WHERE id = ?", lesson2); // xóa ở bản đang soạn

        assertThat(lessonRepository.countLearnerLessons(courseId)).isEqualTo(2);
        assertThat(progressRepository.calculateCourseProgressPercent(userId, courseId)).isEqualTo(50);
        assertThat(progressRepository.countCompletedLessonsByCourse(userId, courseId, ProgressStatus.COMPLETED))
                .isEqualTo(1);
        assertThat(lessonRepository.countLearnerLessonsByCourseIds(List.of(courseId)))
                .singleElement().extracting(LessonRepository.CourseLessonCount::getLessonCount).isEqualTo(2L);
        assertThat(jdbc.queryForObject("SELECT in_published_version FROM lessons WHERE id = ?", Boolean.class, draftOnly))
                .isFalse();
    }

    @Test
    void republish_movesFlagsToNewLessonSet() {
        publishWith(List.of(lesson1, lesson2));
        long lesson3 = insertLesson(3);
        publishWith(List.of(lesson1, lesson3));

        assertThat(jdbc.queryForList("SELECT id FROM lessons WHERE course_id = ? AND in_published_version ORDER BY id",
                Long.class, courseId)).containsExactly(lesson1, lesson3);
        assertThat(versionRepository.findMaxVersionNo(courseId)).isEqualTo(2);
        assertThat(progressRepository.calculateCourseProgressPercent(userId, courseId)).isEqualTo(50);
    }

    @Test
    void publishedLessonJson_roundTripsThroughJsonb() {
        long versionId = publishWith(List.of(lesson1));

        assertThat(publishedLessonRepository.findByPublishedVersionIdAndLessonId(versionId, lesson1))
                .get().extracting(CoursePublishedLesson::getContent).asString().contains("schemaVersion");
        assertThat(publishedLessonRepository.findByPublishedVersionIdAndLessonId(versionId, lesson2)).isEmpty();
    }

    @Test
    void versionNo_uniquePerCourse() {
        versionRepository.saveAndFlush(CoursePublishedVersion.builder().courseId(courseId).versionNo(1).content("{}").build());
        // Để cuối: lỗi ràng buộc làm hỏng giao dịch của test
        assertThatThrownBy(() -> versionRepository.saveAndFlush(
                CoursePublishedVersion.builder().courseId(courseId).versionNo(1).content("{}").build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
