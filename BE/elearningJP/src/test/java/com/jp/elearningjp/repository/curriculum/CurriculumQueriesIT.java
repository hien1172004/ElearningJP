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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Chạy các query gộp và soft-delete hàng loạt trên Postgres thật, qua toàn bộ migration. Cần Docker. */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CurriculumQueriesIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired CourseRepository courseRepository;
    @Autowired LessonRepository lessonRepository;
    @Autowired QuizRepository quizRepository;
    @Autowired CourseEnrollmentRepository enrollmentRepository;
    @Autowired UserLessonProgressRepository progressRepository;
    @Autowired JdbcTemplate jdbc;

    private long userId;
    private long courseId;
    private long[] lessonIds;

    private long insertCourse(String slug, boolean deleted) {
        return jdbc.queryForObject(
                "INSERT INTO courses (title, slug, jlpt_level, deleted) VALUES ('C', ?, 'N5', ?) RETURNING id",
                Long.class, slug, deleted);
    }

    private long insertLesson(long course, int order, boolean deleted) {
        return jdbc.queryForObject(
                "INSERT INTO lessons (course_id, title, order_index, deleted) VALUES (?, 'L', ?, ?) RETURNING id",
                Long.class, course, order, deleted);
    }

    private void insertProgress(long lesson, String status) {
        jdbc.update("INSERT INTO user_lesson_progress (user_id, lesson_id, status) VALUES (?, ?, ?)",
                userId, lesson, status);
    }

    @BeforeEach
    void seed() {
        userId = jdbc.queryForObject(
                "INSERT INTO users (email, password_hash, full_name) VALUES (?, 'x', 'T') RETURNING id",
                Long.class, "curriculum-" + System.nanoTime() + "@test.local");
        courseId = insertCourse("course-" + System.nanoTime(), false);
        lessonIds = new long[]{
                insertLesson(courseId, 1, false),
                insertLesson(courseId, 2, false),
                insertLesson(courseId, 3, false),
                insertLesson(courseId, 4, true)
        };
        insertProgress(lessonIds[0], "COMPLETED");
        insertProgress(lessonIds[1], "COMPLETED");
        insertProgress(lessonIds[2], "IN_PROGRESS");
        insertProgress(lessonIds[3], "COMPLETED"); // bài đã xoá không được tính
    }

    @Test
    void existsBySlug_includesSoftDeleted() {
        String slug = "deleted-" + System.nanoTime();
        insertCourse(slug, true);
        assertThat(courseRepository.existsBySlug(slug)).isTrue();
        assertThat(courseRepository.existsBySlugAndDeletedFalse(slug)).isFalse();
    }

    @Test
    void countActiveLessons_ignoresDeletedAndEmptyCourses() {
        long empty = insertCourse("empty-" + System.nanoTime(), false);
        List<LessonRepository.CourseLessonCount> rows =
                lessonRepository.countActiveLessonsByCourseIds(List.of(courseId, empty));
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getCourseId()).isEqualTo(courseId);
        assertThat(rows.get(0).getLessonCount()).isEqualTo(3);
    }

    @Test
    void batchProgressPercent_matchesSingleCourseQuery() {
        // 2/3 = 66,67 → phải làm tròn giống CAST(... AS INTEGER) của Postgres, tức 67.
        assertThat(progressRepository.calculateCourseProgressPercents(userId, List.of(courseId)))
                .containsEntry(courseId, progressRepository.calculateCourseProgressPercent(userId, courseId))
                .containsEntry(courseId, 67);
    }

    @Test
    void findLessonIdsWithQuiz_onlyActiveQuizzes() {
        jdbc.update("INSERT INTO lesson_quizzes (lesson_id, title, passing_score_percent) VALUES (?, 'Q', 80)",
                lessonIds[0]);
        jdbc.update("INSERT INTO lesson_quizzes (lesson_id, title, passing_score_percent, deleted) VALUES (?, 'Q', 80, TRUE)",
                lessonIds[1]);
        assertThat(quizRepository.findLessonIdsWithQuiz(List.of(lessonIds[0], lessonIds[1], lessonIds[2])))
                .containsExactly(lessonIds[0]);
    }

    @Test
    void softDeleteByCourse_clearsEnrollmentsAndProgress() {
        jdbc.update("INSERT INTO course_enrollments (user_id, course_id, status) VALUES (?, ?, 'ACTIVE')",
                userId, courseId);

        assertThat(enrollmentRepository.softDeleteByCourseId(courseId, Instant.now())).isEqualTo(1);
        assertThat(progressRepository.softDeleteByCourseId(courseId, Instant.now())).isEqualTo(4);

        assertThat(enrollmentRepository.existsByUserIdAndCourseIdAndDeletedFalse(userId, courseId)).isFalse();
        assertThat(progressRepository.findAllByUserIdAndCourseId(userId, courseId)).isEmpty();
    }

    @Test
    void completedEnrollment_countsAsEnrolled() {
        jdbc.update("INSERT INTO course_enrollments (user_id, course_id, status) VALUES (?, ?, 'COMPLETED')",
                userId, courseId);
        assertThat(enrollmentRepository.existsByUserIdAndCourseIdAndDeletedFalse(userId, courseId)).isTrue();
    }

    @Test
    void enrollmentOfDeletedCourse_hiddenFromAllReads() {
        long deletedCourse = insertCourse("gone-" + System.nanoTime(), true);
        Long hidden = jdbc.queryForObject(
                "INSERT INTO course_enrollments (user_id, course_id, status) VALUES (?, ?, 'ACTIVE') RETURNING id",
                Long.class, userId, deletedCourse);
        Long visible = jdbc.queryForObject(
                "INSERT INTO course_enrollments (user_id, course_id, status) VALUES (?, ?, 'ACTIVE') RETURNING id",
                Long.class, userId, courseId);

        assertThat(enrollmentRepository.findByUser_IdAndDeletedFalseAndCourse_DeletedFalseOrderByCreatedAtDesc(userId))
                .extracting(e -> e.getId()).containsExactly(visible);
        assertThat(enrollmentRepository.findByUser_IdAndStatusAndDeletedFalseAndCourse_DeletedFalseOrderByCreatedAtDesc(
                userId, com.jp.elearningjp.shared.enums.EnrollmentStatus.ACTIVE))
                .extracting(e -> e.getId()).containsExactly(visible);
        assertThat(enrollmentRepository.findByIdAndDeletedFalseAndCourse_DeletedFalse(hidden)).isEmpty();
        assertThat(enrollmentRepository.findByIdAndDeletedFalseAndCourse_DeletedFalse(visible)).isPresent();
    }

    @Test
    void countCompleted_ignoresDeletedLessons() {
        // seed: 2 bài active COMPLETED + 1 bài đã xoá COMPLETED → chỉ được tính 2 / tổng 3.
        assertThat(progressRepository.countCompletedLessonsByCourse(
                userId, courseId, com.jp.elearningjp.shared.enums.ProgressStatus.COMPLETED)).isEqualTo(2);
        assertThat(progressRepository.findByUserAndCourseWithLesson(userId, courseId)).hasSize(3);
    }

    @Test
    void insertIfAbsent_isIdempotent_andRowIsLockable() {
        long fresh = insertLesson(courseId, 5, false);

        assertThat(progressRepository.insertIfAbsent(userId, fresh)).isEqualTo(1);
        assertThat(progressRepository.insertIfAbsent(userId, fresh)).isZero();
        assertThat(progressRepository.insertIfAbsent(userId, lessonIds[0])).isZero(); // đã có từ seed

        assertThat(progressRepository.findActiveForUpdate(userId, fresh))
                .get()
                .extracting(p -> p.getStatus())
                .isEqualTo(com.jp.elearningjp.shared.enums.ProgressStatus.NOT_STARTED);
    }

    @Test
    void softDeleteByLesson_affectsOnlyThatLesson() {
        assertThat(progressRepository.softDeleteByLessonId(lessonIds[0], Instant.now())).isEqualTo(1);
        assertThat(progressRepository.findAllByUserIdAndCourseId(userId, courseId)).hasSize(3);
    }
}
