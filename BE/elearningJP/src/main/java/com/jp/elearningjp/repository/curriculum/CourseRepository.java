package com.jp.elearningjp.repository.curriculum;

import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.shared.enums.JlptLevel;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course> {

    Page<Course> findByPublishedTrueAndDeletedFalse(Pageable pageable);

    Page<Course> findByJlptLevelAndPublishedTrueAndDeletedFalse(JlptLevel level, Pageable pageable);

    Page<Course> findByDeletedFalse(Pageable pageable);

    /** Khóa (chưa xóa) có thay đổi chưa duyệt — bộ lọc trang quản lý của ADMIN. */
    Page<Course> findByDeletedFalseAndHasUnpublishedChangesTrue(Pageable pageable);

    /** Khóa học (chưa xóa) được giao cho một giáo viên — dùng cho trang quản lý của TEACHER. */
    @Query(value = """
            SELECT c FROM Course c
            WHERE c.deleted = false
              AND EXISTS (SELECT 1 FROM CourseTeacher ct WHERE ct.course = c AND ct.teacher.id = :teacherId)
            """,
            countQuery = """
            SELECT COUNT(c) FROM Course c
            WHERE c.deleted = false
              AND EXISTS (SELECT 1 FROM CourseTeacher ct WHERE ct.course = c AND ct.teacher.id = :teacherId)
            """)
    Page<Course> findAssignedToTeacher(@Param("teacherId") Long teacherId, Pageable pageable);

    /** Như {@link #findAssignedToTeacher} nhưng chỉ khóa có thay đổi chưa duyệt. */
    @Query(value = """
            SELECT c FROM Course c
            WHERE c.deleted = false AND c.hasUnpublishedChanges = true
              AND EXISTS (SELECT 1 FROM CourseTeacher ct WHERE ct.course = c AND ct.teacher.id = :teacherId)
            """,
            countQuery = """
            SELECT COUNT(c) FROM Course c
            WHERE c.deleted = false AND c.hasUnpublishedChanges = true
              AND EXISTS (SELECT 1 FROM CourseTeacher ct WHERE ct.course = c AND ct.teacher.id = :teacherId)
            """)
    Page<Course> findAssignedToTeacherWithPendingChanges(@Param("teacherId") Long teacherId, Pageable pageable);

    Optional<Course> findByIdAndDeletedFalse(Long id);

    /**
     * Đánh dấu bản đang soạn có thay đổi chưa duyệt. Bulk update (không tăng {@code version})
     * để nhiều giáo viên sửa các bài khác nhau cùng lúc không xung đột optimistic lock trên khóa.
     */
    @Modifying
    @Query("UPDATE Course c SET c.hasUnpublishedChanges = true WHERE c.id = :id AND c.hasUnpublishedChanges = false")
    int markUnpublishedChanges(@Param("id") Long id);

    /**
     * Đánh dấu bản đang soạn có thay đổi + ghi người / thời điểm sửa gần nhất. Bulk update nên không
     * tăng {@code @Version} của khóa (các giáo viên sửa những bài khác nhau không xung đột optimistic lock).
     */
    @Modifying
    @Query("UPDATE Course c SET c.hasUnpublishedChanges = true, c.draftUpdatedById = :userId, "
            + "c.draftUpdatedByName = :userName, c.draftUpdatedAt = :at WHERE c.id = :id")
    int markDraftEdited(@Param("id") Long id, @Param("userId") Long userId,
                        @Param("userName") String userName, @Param("at") Instant at);

    /**
     * Khóa dòng khóa học tới hết giao dịch (SELECT … FOR UPDATE) — dùng khi đưa bài lên bản live để
     * 2 thao tác duyệt / lưu cùng khóa xếp hàng (cùng tính version_no mới, cùng đổi published_version_id).
     * Gọi TRƯỚC khi nạp entity khóa trong giao dịch để entity đọc được dữ liệu mới nhất.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Course c WHERE c.id = :id AND c.deleted = false")
    Optional<Course> findByIdForUpdate(@Param("id") Long id);

    Optional<Course> findBySlugAndDeletedFalse(String slug);

    boolean existsBySlugAndDeletedFalse(String slug);

    /** Tính cả khóa đã xoá mềm — khớp ràng buộc UNIQUE toàn bảng của cột slug. */
    boolean existsBySlug(String slug);

    /** Id khóa đang xuất bản nhưng chưa có snapshot (dữ liệu trước V56). */
    @Query("SELECT c.id FROM Course c WHERE c.published = true AND c.deleted = false AND c.publishedVersionId IS NULL")
    java.util.List<Long> findPublishedIdsWithoutSnapshot();
}
