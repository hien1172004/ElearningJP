package com.jp.elearningjp.repository.curriculum;

import java.time.Instant;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.EntityGraph;
import com.jp.elearningjp.entity.curriculum.CourseEnrollment;
import com.jp.elearningjp.shared.enums.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseEnrollmentRepository extends JpaRepository<CourseEnrollment, Long> {

    boolean existsByUserIdAndCourseIdAndDeletedFalse(Long userId, Long courseId);

    boolean existsByUserIdAndCourseIdAndStatusAndDeletedFalse(Long userId, Long courseId, EnrollmentStatus status);

    Optional<CourseEnrollment> findByUserIdAndCourseIdAndDeletedFalse(Long userId, Long courseId);

    // Nested property traversal (findByUser_IdAndCourse_IdAnd...) — Spring Data JPA hỗ trợ
    Optional<CourseEnrollment> findByUser_IdAndCourse_IdAndDeletedFalse(Long userId, Long courseId);

    Optional<CourseEnrollment> findByUser_IdAndCourse_IdAndStatusAndDeletedFalse(
            Long userId, Long courseId, EnrollmentStatus status);

    boolean existsByUser_IdAndCourse_IdAndStatusAndDeletedFalse(
            Long userId, Long courseId, EnrollmentStatus status);

    @EntityGraph(attributePaths = "course")
    List<CourseEnrollment> findByUser_IdAndDeletedFalseAndCourse_DeletedFalseOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = "course")
    List<CourseEnrollment> findByUser_IdAndStatusAndDeletedFalseAndCourse_DeletedFalseOrderByCreatedAtDesc(
            Long userId, EnrollmentStatus status);

    List<CourseEnrollment> findByUserIdAndDeletedFalse(Long userId);

    Optional<CourseEnrollment> findByIdAndDeletedFalseAndCourse_DeletedFalse(Long id);

    @Modifying
    @Query("""
            UPDATE CourseEnrollment e SET e.deleted = true, e.deletedAt = :now
            WHERE e.course.id = :courseId AND e.deleted = false
            """)
    int softDeleteByCourseId(@Param("courseId") Long courseId, @Param("now") Instant now);

    // =========================================================
    // THEO DÕI HỌC VIÊN (trang giáo viên)
    // Tiến độ chỉ tính bài học chưa xóa của khóa; học viên là tài khoản chưa xóa.
    // =========================================================

    String STUDENT_PROGRESS_FROM = """
            FROM course_enrollments e
            JOIN users u ON u.id = e.user_id AND u.deleted = false
            LEFT JOIN user_lesson_progress p ON p.user_id = e.user_id AND p.deleted = false
                 AND p.lesson_id IN (SELECT l.id FROM lessons l WHERE l.course_id = e.course_id AND l.deleted = false)
            WHERE e.course_id = :courseId AND e.deleted = false
              AND (:keyword = '' OR LOWER(u.full_name) LIKE :keyword OR LOWER(u.email) LIKE :keyword)
            """;

    /**
     * Học viên của khóa kèm tiến độ. Mỗi dòng:
     * [user_id, full_name, email, avatar_url, enrollment_status, enrolled_at,
     *  số bài hoàn thành, số bài đang học, lần học gần nhất, điểm quiz trung bình].
     *
     * <p>{@code keyword}: "" để bỏ lọc, ngược lại dạng "%từ khóa%" chữ thường.
     * {@code sort}: PROGRESS | NAME | ENROLLED | (khác) = học gần nhất trước.</p>
     */
    @Query(value = """
            SELECT e.user_id, u.full_name, u.email, u.avatar_url, e.status, e.created_at,
                   COUNT(p.id) FILTER (WHERE p.status = 'COMPLETED')   AS completed,
                   COUNT(p.id) FILTER (WHERE p.status = 'IN_PROGRESS') AS in_progress,
                   MAX(p.last_accessed_at)                              AS last_active,
                   AVG(p.best_score)                                    AS avg_score
            """ + STUDENT_PROGRESS_FROM + """
            GROUP BY e.id, e.user_id, u.full_name, u.email, u.avatar_url, e.status, e.created_at
            ORDER BY
                CASE WHEN :sort = 'PROGRESS' THEN COUNT(p.id) FILTER (WHERE p.status = 'COMPLETED') END DESC NULLS LAST,
                CASE WHEN :sort = 'NAME' THEN LOWER(u.full_name) END ASC,
                CASE WHEN :sort = 'ENROLLED' THEN e.created_at END DESC,
                MAX(p.last_accessed_at) DESC NULLS LAST,
                e.created_at DESC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM course_enrollments e
            JOIN users u ON u.id = e.user_id AND u.deleted = false
            WHERE e.course_id = :courseId AND e.deleted = false
              AND (:keyword = '' OR LOWER(u.full_name) LIKE :keyword OR LOWER(u.email) LIKE :keyword)
            """,
            nativeQuery = true)
    org.springframework.data.domain.Page<Object[]> findStudentProgress(
            @Param("courseId") Long courseId,
            @Param("keyword") String keyword,
            @Param("sort") String sort,
            org.springframework.data.domain.Pageable pageable);

    /**
     * Tổng quan khóa: 1 dòng [số học viên, số đã học xong mọi bài, số chưa bắt đầu,
     * số học trong khoảng gần đây (từ {@code since}), số bài hoàn thành trung bình].
     */
    @Query(value = """
            SELECT COUNT(*),
                   COUNT(*) FILTER (WHERE :totalLessons > 0 AND s.completed >= :totalLessons),
                   COUNT(*) FILTER (WHERE s.started = 0),
                   COUNT(*) FILTER (WHERE s.last_active >= :since),
                   COALESCE(AVG(s.completed), 0)
            FROM (
                SELECT e.user_id,
                       COUNT(p.id) FILTER (WHERE p.status = 'COMPLETED')                    AS completed,
                       COUNT(p.id) FILTER (WHERE p.status IN ('IN_PROGRESS', 'COMPLETED'))   AS started,
                       MAX(p.last_accessed_at)                                               AS last_active
                FROM course_enrollments e
                JOIN users u ON u.id = e.user_id AND u.deleted = false
                LEFT JOIN user_lesson_progress p ON p.user_id = e.user_id AND p.deleted = false
                     AND p.lesson_id IN (SELECT l.id FROM lessons l WHERE l.course_id = e.course_id AND l.deleted = false)
                WHERE e.course_id = :courseId AND e.deleted = false
                GROUP BY e.user_id
            ) s
            """, nativeQuery = true)
    List<Object[]> summarizeCourseStudents(@Param("courseId") Long courseId,
                                           @Param("totalLessons") int totalLessons,
                                           @Param("since") Instant since);
}
