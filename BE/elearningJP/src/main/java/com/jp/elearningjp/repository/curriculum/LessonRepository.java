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

    @Query("SELECT COALESCE(MAX(l.orderIndex), 0) FROM Lesson l WHERE l.course.id = :courseId AND l.deleted = false")
    Integer findMaxOrderIndexByCourseId(@Param("courseId") Long courseId);
}
