package com.jp.elearningjp.repository.curriculum;

import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.repository.comment.CommentReportRepository;
import com.jp.elearningjp.service.curriculum.CourseService;
import com.jp.elearningjp.shared.enums.JlptLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Phân công giáo viên (V54 course_teachers) trên Postgres thật, qua toàn bộ migration:
 * danh sách khóa được giao, ràng buộc chống trùng và phạm vi kiểm duyệt bình luận. Cần Docker.
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CourseTeacherQueriesIT {

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
    @Autowired CourseTeacherRepository courseTeacherRepository;
    @Autowired CommentReportRepository commentReportRepository;
    @Autowired JdbcTemplate jdbc;

    private long teacherA;
    private long teacherB;
    private long assignedCourse;
    private long otherCourse;

    private long insertUser(String prefix) {
        return jdbc.queryForObject(
                "INSERT INTO users (email, password_hash, full_name) VALUES (?, 'x', 'T') RETURNING id",
                Long.class, prefix + "-" + System.nanoTime() + "@test.local");
    }

    private long insertCourse(boolean deleted) {
        return jdbc.queryForObject(
                "INSERT INTO courses (title, slug, jlpt_level, deleted) VALUES ('C', ?, 'N5', ?) RETURNING id",
                Long.class, "course-" + System.nanoTime(), deleted);
    }

    private void assign(long course, long teacher) {
        jdbc.update("INSERT INTO course_teachers (course_id, teacher_id) VALUES (?, ?)", course, teacher);
    }

    /** Bình luận trong 1 bài của khóa, kèm 1 báo cáo OPEN. */
    private void reportedLessonComment(long course, long reporter) {
        long lesson = jdbc.queryForObject(
                "INSERT INTO lessons (course_id, title, order_index) VALUES (?, 'L', 1) RETURNING id",
                Long.class, course);
        long comment = jdbc.queryForObject("""
                INSERT INTO comments (target_type, target_id, user_id, content)
                VALUES ('LESSON', ?, ?, 'vi phạm') RETURNING id""", Long.class, lesson, reporter);
        jdbc.update("INSERT INTO comment_reports (reporter_id, comment_id, reason) VALUES (?, ?, 'SPAM')",
                reporter, comment);
    }

    @BeforeEach
    void seed() {
        teacherA = insertUser("teacher-a");
        teacherB = insertUser("teacher-b");
        assignedCourse = insertCourse(false);
        otherCourse = insertCourse(false);
        long deletedCourse = insertCourse(true);
        assign(assignedCourse, teacherA);
        assign(assignedCourse, teacherB);
        assign(otherCourse, teacherB);
        assign(deletedCourse, teacherA);
    }

    @Test
    void pendingChangesFilter_adminAndTeacherVariants() {
        jdbc.update("UPDATE courses SET has_unpublished_changes = (id = ?) WHERE id IN (?, ?)",
                otherCourse, assignedCourse, otherCourse);

        // ADMIN: mọi khóa (chưa xóa) có thay đổi chưa duyệt
        assertThat(courseRepository.findByDeletedFalseAndHasUnpublishedChangesTrue(PageRequest.of(0, 50)).getContent())
                .extracting(Course::getId).contains(otherCourse).doesNotContain(assignedCourse);
        // TEACHER: chỉ trong khóa được giao
        assertThat(courseRepository.findAssignedToTeacherWithPendingChanges(teacherA, PageRequest.of(0, 10))
                .getTotalElements()).isZero();
        assertThat(courseRepository.findAssignedToTeacherWithPendingChanges(teacherB, PageRequest.of(0, 10)).getContent())
                .extracting(Course::getId).containsExactly(otherCourse);
    }

    @Test
    void adminListSpec_filtersByKeywordLevelPublished_withinTeacherScope() {
        jdbc.update("UPDATE courses SET title = 'Khóa Ngữ pháp N5', is_published = true WHERE id = ?", assignedCourse);
        jdbc.update("UPDATE courses SET title = 'Kanji N4', jlpt_level = 'N4', is_published = false WHERE id = ?", otherCourse);
        PageRequest page = PageRequest.of(0, 10);

        // TEACHER B được giao cả 2 khóa: lọc theo từ khóa (không phân biệt hoa thường)
        assertThat(courseRepository.findAll(CourseService.adminListSpec(teacherB, false, "ngữ PHÁP", null, null), page)
                .getContent()).extracting(Course::getId).containsExactly(assignedCourse);
        // Lọc cấp độ + trạng thái xuất bản
        assertThat(courseRepository.findAll(CourseService.adminListSpec(teacherB, false, null, JlptLevel.N4, false), page)
                .getContent()).extracting(Course::getId).containsExactly(otherCourse);
        // TEACHER A không thấy khóa không được giao dù khớp từ khóa
        assertThat(courseRepository.findAll(CourseService.adminListSpec(teacherA, false, "kanji", null, null), page)
                .getTotalElements()).isZero();
        // ADMIN (teacherId = null): không lấy khóa đã xóa
        assertThat(courseRepository.findAll(CourseService.adminListSpec(null, false, null, JlptLevel.N5, null), page)
                .getContent()).extracting(Course::getId).contains(assignedCourse).doesNotContain(otherCourse)
                .allMatch(id -> !jdbc.queryForObject("SELECT deleted FROM courses WHERE id = ?", Boolean.class, id));
    }

    @Test
    void findAssignedToTeacher_onlyAssignedAndNotDeleted() {
        Page<Course> page = courseRepository.findAssignedToTeacher(teacherA, PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(Course::getId).containsExactly(assignedCourse);
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(courseRepository.findAssignedToTeacher(teacherB, PageRequest.of(0, 10)).getTotalElements())
                .isEqualTo(2);
    }

    @Test
    void existsAndTeacherIds_reflectAssignments() {
        assertThat(courseTeacherRepository.existsByCourseIdAndTeacherId(assignedCourse, teacherA)).isTrue();
        assertThat(courseTeacherRepository.existsByCourseIdAndTeacherId(otherCourse, teacherA)).isFalse();
        assertThat(courseTeacherRepository.findTeacherIdsByCourseId(assignedCourse))
                .containsExactlyInAnyOrder(teacherA, teacherB);
        assertThat(courseTeacherRepository.findByCourseIdWithTeacher(assignedCourse)).hasSize(2);

        jdbc.update("UPDATE users SET deleted = true WHERE id = ?", teacherB);
        assertThat(courseTeacherRepository.findTeacherIdsByCourseId(assignedCourse)).containsExactly(teacherA);
    }

    @Test
    void duplicateAssignment_rejectedByUniqueConstraint() {
        assertThatThrownBy(() -> assign(assignedCourse, teacherA))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void reportGroups_teacherScope_onlyAssignedCourses() {
        long reporter = insertUser("reporter");
        reportedLessonComment(assignedCourse, reporter);
        reportedLessonComment(otherCourse, reporter);

        assertThat(commentReportRepository.findReportGroups("OPEN", teacherA, PageRequest.of(0, 10))
                .getTotalElements()).isEqualTo(1);
        assertThat(commentReportRepository.findReportGroups("OPEN", teacherB, PageRequest.of(0, 10))
                .getTotalElements()).isEqualTo(2);
        assertThat(commentReportRepository.findReportGroups("OPEN", null, PageRequest.of(0, 10))
                .getTotalElements()).isGreaterThanOrEqualTo(2);
    }
}
