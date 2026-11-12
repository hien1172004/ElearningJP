package com.jp.elearningjp.repository.curriculum;

import java.util.Collection;
import com.jp.elearningjp.entity.curriculum.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {

    List<Lesson> findByCourseIdAndDeletedFalseOrderByOrderIndexAsc(Long courseId);

    Optional<Lesson> findByIdAndDeletedFalse(Long id);

    long countByCourseIdAndDeletedFalse(Long courseId);

    long countByCourse_IdAndDeletedFalse(Long courseId);

    /** Đếm số bài active theo từng khóa; khóa không có bài sẽ không có dòng trả về. */
    @Query("""
            SELECT l.course.id AS courseId, COUNT(l.id) AS lessonCount
            FROM Lesson l
            WHERE l.course.id IN :courseIds AND l.deleted = false
            GROUP BY l.course.id
            """)
    List<CourseLessonCount> countActiveLessonsByCourseIds(@Param("courseIds") Collection<Long> courseIds);

    interface CourseLessonCount {
        Long getCourseId();

        Long getLessonCount();
    }

    /** Số bài học viên đang học theo từng khóa (xem {@link UserLessonProgressRepository#LEARNER_LESSON_L}). */
    @Query("SELECT l.course.id AS courseId, COUNT(l.id) AS lessonCount FROM Lesson l "
            + "WHERE l.course.id IN :courseIds AND " + UserLessonProgressRepository.LEARNER_LESSON_L
            + " GROUP BY l.course.id")
    List<CourseLessonCount> countLearnerLessonsByCourseIds(@Param("courseIds") Collection<Long> courseIds);

    /** Số bài học viên đang học của 1 khóa. */
    @Query("SELECT COUNT(l.id) FROM Lesson l WHERE l.course.id = :courseId AND "
            + UserLessonProgressRepository.LEARNER_LESSON_L)
    long countLearnerLessons(@Param("courseId") Long courseId);

    /**
     * Đánh dấu bài có trong phiên bản xuất bản mới nhất (gọi khi tạo snapshot). Bài đã lên bản học viên thì cả hai
     * phía đều thấy (V66: created_kind về null).
     */
    @Modifying
    @Query(value = "UPDATE lessons SET in_published_version = (id IN (:lessonIds)), "
            + "created_kind = CASE WHEN id IN (:lessonIds) THEN NULL ELSE created_kind END WHERE course_id = :courseId",
            nativeQuery = true)
    int markPublishedLessons(@Param("courseId") Long courseId, @Param("lessonIds") Collection<Long> lessonIds);

    /** Nội dung live của các bài vừa đổi (V65): tăng {@code live_revision} → nháp dựa trên số cũ thành nháp đã cũ. */
    @Modifying
    @Query(value = "UPDATE lessons SET live_revision = live_revision + 1 WHERE id IN (:ids)", nativeQuery = true)
    int bumpLiveRevision(@Param("ids") Collection<Long> ids);

    @Query(value = "SELECT live_revision FROM lessons WHERE id = :id", nativeQuery = true)
    Optional<Long> findLiveRevision(@Param("id") Long id);

    /**
     * Danh sách bài của khóa mà phía {@code kind} thấy (V66 — mục lục, sắp xếp, lưu cả khóa): bài đã lên bản học viên
     * hoặc bài do chính phía này tạo. Bài mới phía bên kia tạo chưa được duyệt / lưu bị ẩn — kể cả khi giáo viên đã gửi
     * duyệt (phải duyệt mới vào danh sách bài).
     */
    @Query("SELECT l FROM Lesson l WHERE l.course.id = :courseId AND l.deleted = false "
            + "AND (l.inPublishedVersion = true OR l.createdKind IS NULL OR l.createdKind = :kind) ORDER BY l.orderIndex ASC")
    List<Lesson> findVisibleByCourseId(@Param("courseId") Long courseId,
                                       @Param("kind") com.jp.elearningjp.shared.enums.DraftKind kind);

    /**
     * Bài hiện ở khu duyệt của phía {@code kind} ("Duyệt theo bài", "Thay đổi chờ duyệt"): như
     * {@link #findVisibleByCourseId} và thêm (với ADMIN) bài mới giáo viên <b>đã gửi duyệt</b> — để xem bản gửi và
     * duyệt. Bài mới giáo viên còn đang soạn vẫn bị ẩn.
     */
    @Query("SELECT l FROM Lesson l WHERE l.course.id = :courseId AND l.deleted = false "
            + "AND (l.inPublishedVersion = true OR l.createdKind IS NULL OR l.createdKind = :kind "
            + "OR (:kind = com.jp.elearningjp.shared.enums.DraftKind.ADMIN AND EXISTS (SELECT 1 FROM LessonReviewRequest r "
            + "WHERE r.lesson = l AND r.status = com.jp.elearningjp.shared.enums.ReviewRequestStatus.PENDING))) "
            + "ORDER BY l.orderIndex ASC")
    List<Lesson> findReviewableByCourseId(@Param("courseId") Long courseId,
                                          @Param("kind") com.jp.elearningjp.shared.enums.DraftKind kind);

    /** Id các bài chưa xóa của khóa. */
    @Query("SELECT l.id FROM Lesson l WHERE l.course.id = :courseId AND l.deleted = false")
    List<Long> findActiveIdsByCourseId(@Param("courseId") Long courseId);

    /** Khóa xuất bản không có bài nào. */
    @Modifying
    @Query(value = "UPDATE lessons SET in_published_version = false WHERE course_id = :courseId", nativeQuery = true)
    int clearPublishedLessons(@Param("courseId") Long courseId);

    // =========================================================
    // DUYỆT THEO BÀI — trạng thái nháp nằm ở lesson_drafts (V65, LessonDraftRepository); các cột V62 trên
    // lessons chỉ còn được dọn khi xuất bản.
    // =========================================================

    /** Bài vừa được đưa lên bản live (duyệt / ADMIN lưu): hết thay đổi chưa duyệt, mở khóa sửa. */
    @Modifying
    @Query(value = """
            UPDATE lessons SET review_status = 'EDITING', has_unpublished_changes = FALSE
            WHERE id IN (:ids)
            """, nativeQuery = true)
    int markLessonsPublished(@Param("ids") Collection<Long> ids);

    /** Cả khóa vừa được chụp thành bản live (luồng duyệt theo khóa): mọi bài hết thay đổi chưa duyệt. */
    @Modifying
    @Query(value = "UPDATE lessons SET has_unpublished_changes = FALSE WHERE course_id = :courseId", nativeQuery = true)
    int clearLessonChanges(@Param("courseId") Long courseId);

    /** Số bài của khóa đang chờ duyệt theo bài — nháp TEACHER đã gửi (V65; chặn luồng duyệt theo khóa chạy chồng lên). */
    @Query(value = "SELECT COUNT(*) FROM lesson_drafts d JOIN lessons l ON l.id = d.lesson_id "
            + "WHERE l.course_id = :courseId AND l.deleted = FALSE AND d.status = 'SUBMITTED'", nativeQuery = true)
    long countPendingReviewLessons(@Param("courseId") Long courseId);

    @Query("SELECT COALESCE(MAX(l.orderIndex), 0) FROM Lesson l WHERE l.course.id = :courseId AND l.deleted = false")
    Integer findMaxOrderIndexByCourseId(@Param("courseId") Long courseId);
}
