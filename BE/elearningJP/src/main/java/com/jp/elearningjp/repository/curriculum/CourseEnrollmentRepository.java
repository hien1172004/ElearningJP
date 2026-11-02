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
}