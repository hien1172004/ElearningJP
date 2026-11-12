package com.jp.elearningjp.repository.curriculum;

import com.jp.elearningjp.entity.curriculum.LessonReviewRequest;
import com.jp.elearningjp.shared.enums.ReviewRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LessonReviewRequestRepository extends JpaRepository<LessonReviewRequest, Long> {

    /** Chỉ lấy id khóa (không nạp entity) — để khóa dòng khóa học trước khi đọc yêu cầu. */
    @Query("SELECT r.course.id FROM LessonReviewRequest r WHERE r.id = :id")
    Optional<Long> findCourseIdById(@Param("id") Long id);

    /** Yêu cầu đang chờ của 1 bài (tối đa 1 — partial unique index). */
    Optional<LessonReviewRequest> findByLessonIdAndStatus(Long lessonId, ReviewRequestStatus status);

    /** Lượt gửi gần nhất của bài (báo kết quả cho người thao tác sau). */
    @EntityGraph(attributePaths = {"reviewedBy"})
    Optional<LessonReviewRequest> findFirstByLessonIdOrderByCreatedAtDescIdDesc(Long lessonId);

    /** Lịch sử gửi duyệt theo bài của 1 khóa, mới nhất trước. */
    @EntityGraph(attributePaths = {"lesson", "submittedBy", "reviewedBy"})
    Page<LessonReviewRequest> findByCourseIdOrderByCreatedAtDescIdDesc(Long courseId, Pageable pageable);

    /** Yêu cầu đang chờ của các bài trong khóa (hiện trạng thái từng bài). */
    @EntityGraph(attributePaths = {"submittedBy"})
    List<LessonReviewRequest> findByCourseIdAndStatus(Long courseId, ReviewRequestStatus status);

    /** Số bài (chưa xóa) đang chờ duyệt theo từng khóa; khóa không có thì không có dòng. */
    @Query("SELECT r.course.id AS courseId, COUNT(r.id) AS pendingCount FROM LessonReviewRequest r "
            + "WHERE r.course.id IN :courseIds AND r.status = com.jp.elearningjp.shared.enums.ReviewRequestStatus.PENDING "
            + "AND r.lesson.deleted = false GROUP BY r.course.id")
    List<CoursePendingCount> countPendingByCourseIds(@Param("courseIds") java.util.Collection<Long> courseIds);

    interface CoursePendingCount {
        Long getCourseId();

        Long getPendingCount();
    }

    /** Tên các bài (chưa xóa) đang chờ duyệt của khóa, cũ nhất trước — chặn ADMIN sửa khóa (V65). */
    @Query("SELECT r.lesson.title FROM LessonReviewRequest r WHERE r.course.id = :courseId "
            + "AND r.status = com.jp.elearningjp.shared.enums.ReviewRequestStatus.PENDING AND r.lesson.deleted = false "
            + "ORDER BY r.createdAt ASC")
    List<String> findPendingLessonTitles(@Param("courseId") Long courseId);

    /** Hàng chờ duyệt theo bài của ADMIN (khóa và bài chưa xóa), cũ nhất trước. */
    @EntityGraph(attributePaths = {"lesson", "course", "submittedBy", "reviewedBy"})
    @Query(value = """
            SELECT r FROM LessonReviewRequest r JOIN r.course c JOIN r.lesson l
            WHERE r.status = :status AND c.deleted = false AND l.deleted = false
            ORDER BY r.createdAt ASC, r.id ASC
            """,
            countQuery = """
            SELECT COUNT(r) FROM LessonReviewRequest r JOIN r.course c JOIN r.lesson l
            WHERE r.status = :status AND c.deleted = false AND l.deleted = false
            """)
    Page<LessonReviewRequest> findQueue(@Param("status") ReviewRequestStatus status, Pageable pageable);

    /** Như {@link #findQueue} và lọc theo {@code pattern} (đã LOWER, kèm %) trên tên bài, tên khóa, tên người gửi. */
    @EntityGraph(attributePaths = {"lesson", "course", "submittedBy", "reviewedBy"})
    @Query(value = """
            SELECT r FROM LessonReviewRequest r JOIN r.course c JOIN r.lesson l LEFT JOIN r.submittedBy u
            WHERE r.status = :status AND c.deleted = false AND l.deleted = false
              AND (LOWER(l.title) LIKE :pattern OR LOWER(c.title) LIKE :pattern
                   OR LOWER(COALESCE(u.fullName, '')) LIKE :pattern)
            ORDER BY r.createdAt ASC, r.id ASC
            """,
            countQuery = """
            SELECT COUNT(r) FROM LessonReviewRequest r JOIN r.course c JOIN r.lesson l LEFT JOIN r.submittedBy u
            WHERE r.status = :status AND c.deleted = false AND l.deleted = false
              AND (LOWER(l.title) LIKE :pattern OR LOWER(c.title) LIKE :pattern
                   OR LOWER(COALESCE(u.fullName, '')) LIKE :pattern)
            """)
    Page<LessonReviewRequest> searchQueue(@Param("status") ReviewRequestStatus status, @Param("pattern") String pattern,
                                          Pageable pageable);
}
