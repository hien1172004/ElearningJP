package com.jp.elearningjp.repository.curriculum;

import com.jp.elearningjp.entity.curriculum.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
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
