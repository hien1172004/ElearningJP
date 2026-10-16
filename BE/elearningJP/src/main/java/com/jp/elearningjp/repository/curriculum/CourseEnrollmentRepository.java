package com.jp.elearningjp.repository.curriculum;

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

    List<CourseEnrollment> findByUser_IdAndDeletedFalseOrderByCreatedAtDesc(Long userId);

    List<CourseEnrollment> findByUser_IdAndStatusAndDeletedFalseOrderByCreatedAtDesc(
            Long userId, EnrollmentStatus status);

    List<CourseEnrollment> findByUserIdAndDeletedFalse(Long userId);
}