package com.jp.elearningjp.repository.curriculum;

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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Duyệt theo bài trên nháp theo phía (V62 + V65) — câu UPDATE có điều kiện trên Postgres thật, qua toàn bộ migration. Cần Docker. */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class LessonReviewQueriesIT {

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
    @Autowired LessonDraftRepository draftRepository;
    @Autowired LessonReviewRequestRepository requestRepository;
    @Autowired JdbcTemplate jdbc;

    private long courseId;
    private long lessonId;
    private long userId;
    private long teacherDraftId;

    private long insertLesson(long course, int order) {
        return jdbc.queryForObject("INSERT INTO lessons (course_id, title, order_index) VALUES (?, 'L', ?) RETURNING id",
                Long.class, course, order);
    }

    private long insertRequest(long lesson, String status, String createdAt) {
        return jdbc.queryForObject("INSERT INTO lesson_review_requests (lesson_id, course_id, batch_id, submitted_by, "
                        + "draft_revision, status, created_at) VALUES (?, ?, ?, ?, 0, ?, CAST(? AS timestamptz)) RETURNING id",
                Long.class, lesson, courseId, UUID.randomUUID(), userId, status, createdAt);
    }

    private long revision() {
        return jdbc.queryForObject("SELECT draft_revision FROM lesson_drafts WHERE id = ?", Long.class, teacherDraftId);
    }

    private String status() {
        return jdbc.queryForObject("SELECT status FROM lesson_drafts WHERE id = ?", String.class, teacherDraftId);
    }

    @BeforeEach
    void seed() {
        userId = jdbc.queryForObject("INSERT INTO users (email, password_hash, full_name) VALUES (?, 'x', 'T') RETURNING id",
                Long.class, "lr-" + System.nanoTime() + "@test.local");
        courseId = jdbc.queryForObject(
                "INSERT INTO courses (title, slug, jlpt_level) VALUES ('C', ?, 'N5') RETURNING id",
                Long.class, "lr-" + System.nanoTime());
        lessonId = insertLesson(courseId, 1);
        teacherDraftId = jdbc.queryForObject("INSERT INTO lesson_drafts (lesson_id, kind) VALUES (?, 'TEACHER') RETURNING id",
                Long.class, lessonId);
    }

    @Test
    void edit_bumpsRevision_marksChanges_withoutBumpingEntityVersion() {
        long versionBefore = jdbc.queryForObject("SELECT version FROM lesson_drafts WHERE id = ?", Long.class, teacherDraftId);
        Instant at = Instant.parse("2026-11-11T03:32:00Z");

        assertThat(draftRepository.markEdited(teacherDraftId, userId, "Nguyễn A", at)).isEqualTo(1);

        var row = jdbc.queryForMap("SELECT * FROM lesson_drafts WHERE id = ?", teacherDraftId);
        assertThat(row.get("draft_revision")).isEqualTo(1L);
        assertThat(row.get("has_changes")).isEqualTo(true);
        assertThat(row.get("updated_by_name")).isEqualTo("Nguyễn A");
        assertThat(row.get("version")).isEqualTo(versionBefore);
    }

    @Test
    void submit_onlyWithMatchingRevision_thenDraftLocked_unlockReopens() {
        draftRepository.markEdited(teacherDraftId, userId, "A", Instant.now());

        assertThat(draftRepository.lockForReview(teacherDraftId, 0L)).isZero();   // bản cũ → xung đột
        assertThat(draftRepository.lockForReview(teacherDraftId, 1L)).isEqualTo(1);
        assertThat(status()).isEqualTo("SUBMITTED");
        assertThat(draftRepository.markEdited(teacherDraftId, userId, "B", Instant.now())).isZero(); // khóa
        assertThat(lessonRepository.countPendingReviewLessons(courseId)).isEqualTo(1L);

        assertThat(draftRepository.unlockReview(teacherDraftId)).isEqualTo(1);
        assertThat(draftRepository.markEdited(teacherDraftId, userId, "B", Instant.now())).isEqualTo(1);
    }

    @Test
    void submit_withoutChanges_or_outdatedDraft_rejected() {
        assertThat(draftRepository.lockForReview(teacherDraftId, revision())).isZero(); // chưa sửa gì

        draftRepository.markEdited(teacherDraftId, userId, "A", Instant.now());
        lessonRepository.bumpLiveRevision(List.of(lessonId)); // ADMIN vừa lưu bài lên live
        assertThat(draftRepository.lockForReview(teacherDraftId, revision())).isZero(); // nháp đã cũ
    }

    @Test
    void published_clearsChanges_followsNewLive_bumpsRevisionSoStaleSubmitConflicts() {
        draftRepository.markEdited(teacherDraftId, userId, "A", Instant.now());
        long seen = revision();
        lessonRepository.bumpLiveRevision(List.of(lessonId));

        assertThat(draftRepository.markPublished(teacherDraftId, 1L)).isEqualTo(1);

        var row = jdbc.queryForMap("SELECT * FROM lesson_drafts WHERE id = ?", teacherDraftId);
        assertThat(row.get("has_changes")).isEqualTo(false);
        assertThat(row.get("base_live_revision")).isEqualTo(1L);
        assertThat(draftRepository.lockForReview(teacherDraftId, seen)).isZero();
    }

    @Test
    void oneActiveAndOneArchivedDraftPerSide() {
        long admin = jdbc.queryForObject("INSERT INTO lesson_drafts (lesson_id, kind) VALUES (?, 'ADMIN') RETURNING id",
                Long.class, lessonId);
        assertThat(draftRepository.findActive(lessonId, "TEACHER")).get().extracting(d -> d.getId()).isEqualTo(teacherDraftId);
        assertThat(draftRepository.findActive(lessonId, "ADMIN")).get().extracting(d -> d.getId()).isEqualTo(admin);

        assertThat(draftRepository.archive(teacherDraftId)).isEqualTo(1);
        assertThat(draftRepository.findActive(lessonId, "TEACHER")).isEmpty();
        assertThat(draftRepository.findArchived(lessonId, "TEACHER")).isPresent();
        jdbc.update("INSERT INTO lesson_drafts (lesson_id, kind) VALUES (?, 'TEACHER')", lessonId); // nháp mới được
        // Vi phạm cuối cùng: sau lỗi này giao dịch của test bị hủy
        assertThatThrownBy(() -> jdbc.update("INSERT INTO lesson_drafts (lesson_id, kind) VALUES (?, 'TEACHER')", lessonId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_lesson_drafts_active");
    }

    @Test
    void newTeacherLesson_hiddenFromAdmin_untilSubmitted_visibleToBothOnceLive() {
        var teacher = com.jp.elearningjp.shared.enums.DraftKind.TEACHER;
        var admin = com.jp.elearningjp.shared.enums.DraftKind.ADMIN;
        long newLesson = insertLesson(courseId, 2);
        jdbc.update("UPDATE lessons SET created_kind = 'TEACHER' WHERE id = ?", newLesson);
        long adminLesson = insertLesson(courseId, 3);
        jdbc.update("UPDATE lessons SET created_kind = 'ADMIN' WHERE id = ?", adminLesson);

        // Giáo viên đang soạn: ADMIN không thấy bài mới của giáo viên; giáo viên không thấy bài mới của ADMIN
        assertThat(lessonRepository.findVisibleByCourseId(courseId, admin)).extracting(l -> l.getId())
                .containsExactly(lessonId, adminLesson);
        assertThat(lessonRepository.findVisibleByCourseId(courseId, teacher)).extracting(l -> l.getId())
                .containsExactly(lessonId, newLesson);

        assertThat(lessonRepository.findReviewableByCourseId(courseId, admin)).extracting(l -> l.getId())
                .containsExactly(lessonId, adminLesson);

        // Giáo viên gửi duyệt → chỉ hiện ở khu duyệt của ADMIN; danh sách bài vẫn chưa có (phải duyệt mới vào)
        insertRequest(newLesson, "PENDING", "2026-11-12T08:00:00Z");
        assertThat(lessonRepository.findReviewableByCourseId(courseId, admin)).extracting(l -> l.getId())
                .containsExactly(lessonId, newLesson, adminLesson);
        assertThat(lessonRepository.findVisibleByCourseId(courseId, admin)).extracting(l -> l.getId())
                .containsExactly(lessonId, adminLesson);

        // Lên bản học viên → cả hai phía thấy (created_kind về null)
        lessonRepository.markPublishedLessons(courseId, List.of(lessonId, newLesson, adminLesson));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM lessons WHERE course_id = ? AND created_kind IS NOT NULL",
                Integer.class, courseId)).isZero();
    }

    @Test
    void onePendingRequestPerLesson() {
        // Lịch sử (đã đóng) không giới hạn
        insertRequest(lessonId, "REJECTED", "2026-11-11T00:00:00Z");
        insertRequest(lessonId, "WITHDRAWN", "2026-11-11T00:30:00Z");
        insertRequest(lessonId, "PENDING", "2026-11-11T01:00:00Z");
        // Vi phạm cuối cùng: sau lỗi này giao dịch của test bị hủy
        assertThatThrownBy(() -> insertRequest(lessonId, "PENDING", "2026-11-11T02:00:00Z"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_lesson_review_requests_pending");
    }

    @Test
    void queue_oldestFirst_skipsDeletedLesson_andCourseIdLookup() {
        long other = insertLesson(courseId, 2);
        long deleted = insertLesson(courseId, 3);
        long newer = insertRequest(lessonId, "PENDING", "2026-11-11T05:00:00Z");
        long older = insertRequest(other, "PENDING", "2026-11-11T01:00:00Z");
        insertRequest(deleted, "PENDING", "2026-11-11T00:00:00Z");
        jdbc.update("UPDATE lessons SET deleted = true WHERE id = ?", deleted);

        var page = requestRepository.findQueue(ReviewRequestStatus.PENDING, PageRequest.of(0, 10));
        assertThat(page.getContent()).extracting(r -> r.getId()).containsExactly(older, newer);
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(requestRepository.findCourseIdById(newer)).contains(courseId);
        // Tìm trong hàng chờ: theo tên bài / tên khóa / người gửi, không phân biệt hoa thường; bài đã xóa vẫn bị loại
        jdbc.update("UPDATE lessons SET title = 'Chủ đề Giáo dục' WHERE id = ?", other);
        assertThat(requestRepository.searchQueue(ReviewRequestStatus.PENDING, "%giáo dục%", PageRequest.of(0, 10))
                .getContent()).extracting(r -> r.getId()).containsExactly(older);
        assertThat(requestRepository.searchQueue(ReviewRequestStatus.PENDING, "%c%", PageRequest.of(0, 10))
                .getTotalElements()).isEqualTo(2);
        assertThat(requestRepository.searchQueue(ReviewRequestStatus.PENDING, "%t%", PageRequest.of(0, 10))
                .getContent()).extracting(r -> r.getId()).containsExactly(older, newer); // người gửi "T"
        // Đếm bài chờ duyệt theo khóa (nhãn "Có n bản đang chờ duyệt"): bỏ bài đã xóa
        assertThat(requestRepository.countPendingByCourseIds(List.of(courseId)))
                .singleElement().extracting(c -> c.getPendingCount()).isEqualTo(2L);
        assertThat(requestRepository.findPendingLessonTitles(courseId)).hasSize(2);
    }
}
