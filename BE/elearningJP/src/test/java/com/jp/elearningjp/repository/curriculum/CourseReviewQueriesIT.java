package com.jp.elearningjp.repository.curriculum;

import com.jp.elearningjp.entity.curriculum.CourseReviewRequest;
import com.jp.elearningjp.shared.enums.ReviewRequestStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Luồng duyệt (V55) trên Postgres thật, qua toàn bộ migration. Cần Docker. */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CourseReviewQueriesIT {

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
    @Autowired CourseReviewRequestRepository reviewRequestRepository;
    @Autowired CourseReviewCommentRepository commentRepository;
    @Autowired JdbcTemplate jdbc;

    private long courseId;
    private long teacherId;

    private long insertCourse(boolean deleted) {
        return jdbc.queryForObject(
                "INSERT INTO courses (title, slug, jlpt_level, deleted) VALUES ('C', ?, 'N5', ?) RETURNING id",
                Long.class, "course-" + System.nanoTime(), deleted);
    }

    private void insertRequest(long course, String status) {
        jdbc.update("INSERT INTO course_review_requests (course_id, submitted_by, status) VALUES (?, ?, ?)",
                course, teacherId, status);
    }

    private long insertRequestReturningId(long course, String status, String createdAt) {
        return jdbc.queryForObject("INSERT INTO course_review_requests (course_id, submitted_by, status, created_at) "
                + "VALUES (?, ?, ?, CAST(? AS timestamptz)) RETURNING id", Long.class, course, teacherId, status, createdAt);
    }

    @BeforeEach
    void seed() {
        teacherId = jdbc.queryForObject(
                "INSERT INTO users (email, password_hash, full_name) VALUES (?, 'x', 'T') RETURNING id",
                Long.class, "review-" + System.nanoTime() + "@test.local");
        courseId = insertCourse(false);
    }

    @Test
    void newCourse_defaultsToEditingWithoutChanges() {
        assertThat(jdbc.queryForObject("SELECT review_status FROM courses WHERE id = ?", String.class, courseId))
                .isEqualTo("EDITING");
        assertThat(jdbc.queryForObject("SELECT has_unpublished_changes FROM courses WHERE id = ?", Boolean.class, courseId))
                .isFalse();
    }

    @Test
    void markUnpublishedChanges_setsFlagOnce_withoutBumpingVersion() {
        long versionBefore = jdbc.queryForObject("SELECT version FROM courses WHERE id = ?", Long.class, courseId);

        assertThat(courseRepository.markUnpublishedChanges(courseId)).isEqualTo(1);
        assertThat(courseRepository.markUnpublishedChanges(courseId)).isZero(); // đã đánh dấu → không ghi lại

        assertThat(jdbc.queryForObject("SELECT has_unpublished_changes FROM courses WHERE id = ?", Boolean.class, courseId))
                .isTrue();
        assertThat(jdbc.queryForObject("SELECT version FROM courses WHERE id = ?", Long.class, courseId))
                .isEqualTo(versionBefore);
    }

    @Test
    void reviewStatus_checkConstraint() {
        assertThatThrownBy(() -> jdbc.update("UPDATE courses SET review_status = 'BOGUS' WHERE id = ?", courseId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void onlyOnePendingRequestPerCourse_butHistoryAllowed() {
        insertRequest(courseId, "REJECTED");
        insertRequest(courseId, "WITHDRAWN");
        insertRequest(courseId, "PENDING");

        assertThat(reviewRequestRepository.findByCourseIdAndStatus(courseId, ReviewRequestStatus.PENDING)).isPresent();
        assertThat(reviewRequestRepository.findByCourseIdOrderByCreatedAtDesc(courseId)).hasSize(3);
        // Để cuối: lỗi ràng buộc làm hỏng giao dịch của test, không query thêm được
        assertThatThrownBy(() -> insertRequest(courseId, "PENDING"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void queue_onlyPendingOfNotDeletedCourses() {
        long deletedCourse = insertCourse(true);
        insertRequest(courseId, "PENDING");
        insertRequest(deletedCourse, "PENDING");

        var page = reviewRequestRepository.findByStatusAndCourseDeletedFalseOrderByCreatedAtAsc(
                ReviewRequestStatus.PENDING, PageRequest.of(0, 50));

        assertThat(page.getContent()).extracting(r -> r.getCourse().getId())
                .contains(courseId).doesNotContain(deletedCourse);
        assertThat(page.getContent()).extracting(CourseReviewRequest::getSubmittedBy).allMatch(u -> u != null);
    }

    @Test
    void searchQueue_matchesCourseTitleOrSubmitterName_caseInsensitive() {
        jdbc.update("UPDATE courses SET title = 'Khóa Kanji N3' WHERE id = ?", courseId);
        jdbc.update("UPDATE users SET full_name = 'Nguyễn Văn Giáo' WHERE id = ?", teacherId);
        long otherCourse = insertCourse(false);
        insertRequest(courseId, "PENDING");
        insertRequest(otherCourse, "PENDING");
        PageRequest page = PageRequest.of(0, 50);

        assertThat(reviewRequestRepository.searchQueue(ReviewRequestStatus.PENDING, "%kanji%", page).getContent())
                .extracting(r -> r.getCourse().getId()).containsExactly(courseId);
        // Theo tên người gửi: cả 2 yêu cầu đều do giáo viên này gửi
        assertThat(reviewRequestRepository.searchQueue(ReviewRequestStatus.PENDING, "%văn giáo%", page).getTotalElements())
                .isGreaterThanOrEqualTo(2);
        assertThat(reviewRequestRepository.searchQueue(ReviewRequestStatus.APPROVED, "%kanji%", page).getTotalElements())
                .isZero();
    }

    @Test
    void comments_ofLatestRequest_inOrder_withAuthor_andCount() {
        insertRequestReturningId(courseId, "REJECTED", "2026-01-01T00:00:00Z");
        long latest = insertRequestReturningId(courseId, "PENDING", "2026-02-01T00:00:00Z");
        long lessonId = jdbc.queryForObject(
                "INSERT INTO lessons (course_id, title, order_index) VALUES (?, 'L', 1) RETURNING id", Long.class, courseId);
        jdbc.update("INSERT INTO course_review_comments (review_request_id, course_id, lesson_id, content, author_id, created_at) "
                + "VALUES (?, ?, ?, 'Thiếu ví dụ', ?, '2026-02-02T00:00:00Z')", latest, courseId, lessonId, teacherId);
        jdbc.update("INSERT INTO course_review_comments (review_request_id, course_id, content, created_at) "
                + "VALUES (?, ?, 'Chung', '2026-02-01T12:00:00Z')", latest, courseId);

        assertThat(reviewRequestRepository.findFirstByCourseIdOrderByCreatedAtDescIdDesc(courseId))
                .get().extracting(CourseReviewRequest::getId).isEqualTo(latest);
        var comments = commentRepository.findByReviewRequestIdOrderByCreatedAtAsc(latest);
        assertThat(comments).extracting(c -> c.getContent()).containsExactly("Chung", "Thiếu ví dụ");
        assertThat(comments.get(1).getAuthor().getId()).isEqualTo(teacherId);
        assertThat(comments.get(1).getLessonId()).isEqualTo(lessonId);
        assertThat(commentRepository.countByReviewRequestId(latest)).isEqualTo(2);
    }
}
