package com.jp.elearningjp.repository.curriculum;

import java.util.Collection;
import com.jp.elearningjp.entity.curriculum.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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

    /** Đánh dấu bài có trong phiên bản xuất bản mới nhất (gọi khi tạo snapshot). */
    @Modifying
    @Query(value = "UPDATE lessons SET in_published_version = (id IN (:lessonIds)) WHERE course_id = :courseId",
            nativeQuery = true)
    int markPublishedLessons(@Param("courseId") Long courseId, @Param("lessonIds") Collection<Long> lessonIds);

    /** Khóa xuất bản không có bài nào. */
    @Modifying
    @Query(value = "UPDATE lessons SET in_published_version = false WHERE course_id = :courseId", nativeQuery = true)
    int clearPublishedLessons(@Param("courseId") Long courseId);

    @Query("SELECT COALESCE(MAX(l.orderIndex), 0) FROM Lesson l WHERE l.course.id = :courseId AND l.deleted = false")
    Integer findMaxOrderIndexByCourseId(@Param("courseId") Long courseId);

    /**
     * Lấy 1 lesson + tất cả sections (đã sắp xếp).
     * Tránh N+1 khi truy cập lesson.getSections().
     */
    @Query("""
            SELECT DISTINCT l FROM Lesson l
            LEFT JOIN FETCH l.sections s
            WHERE l.id = :id AND l.deleted = false
            ORDER BY s.orderIndex ASC
            """)
    Optional<Lesson> findByIdWithSections(@Param("id") Long id);
}
