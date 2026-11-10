package com.jp.elearningjp.repository.curriculum;

import com.jp.elearningjp.entity.curriculum.CourseReviewRequest;
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
public interface CourseReviewRequestRepository extends JpaRepository<CourseReviewRequest, Long> {

    /** Yêu cầu đang chờ của 1 khóa (tối đa 1 — partial unique index). */
    Optional<CourseReviewRequest> findByCourseIdAndStatus(Long courseId, ReviewRequestStatus status);

    /** Lịch sử gửi duyệt của 1 khóa, mới nhất trước. */
    @EntityGraph(attributePaths = {"submittedBy", "reviewedBy"})
    List<CourseReviewRequest> findByCourseIdOrderByCreatedAtDesc(Long courseId);

    /** Lượt gửi duyệt gần nhất của khóa (để hiện nhận xét cho giáo viên sửa). */
    Optional<CourseReviewRequest> findFirstByCourseIdOrderByCreatedAtDescIdDesc(Long courseId);

    /** Hàng chờ duyệt của ADMIN (khóa chưa xóa), cũ nhất trước. */
    @EntityGraph(attributePaths = {"course", "submittedBy", "reviewedBy"})
    Page<CourseReviewRequest> findByStatusAndCourseDeletedFalseOrderByCreatedAtAsc(
            ReviewRequestStatus status, Pageable pageable);

    /**
     * Hàng chờ duyệt kèm tìm kiếm theo tên khóa hoặc tên người gửi.
     * {@code pattern} đã ở dạng chữ thường và bọc "%" (không null — tránh lỗi suy kiểu NULL của Postgres).
     */
    @EntityGraph(attributePaths = {"course", "submittedBy", "reviewedBy"})
    @Query(value = """
            SELECT r FROM CourseReviewRequest r JOIN r.course c LEFT JOIN r.submittedBy u
            WHERE r.status = :status AND c.deleted = false
              AND (LOWER(c.title) LIKE :pattern OR LOWER(COALESCE(u.fullName, '')) LIKE :pattern)
            ORDER BY r.createdAt ASC
            """,
            countQuery = """
            SELECT COUNT(r) FROM CourseReviewRequest r JOIN r.course c LEFT JOIN r.submittedBy u
            WHERE r.status = :status AND c.deleted = false
              AND (LOWER(c.title) LIKE :pattern OR LOWER(COALESCE(u.fullName, '')) LIKE :pattern)
            """)
    Page<CourseReviewRequest> searchQueue(@Param("status") ReviewRequestStatus status,
                                          @Param("pattern") String pattern, Pageable pageable);
}
