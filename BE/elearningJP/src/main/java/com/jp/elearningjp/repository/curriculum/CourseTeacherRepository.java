package com.jp.elearningjp.repository.curriculum;

import com.jp.elearningjp.entity.curriculum.CourseTeacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseTeacherRepository extends JpaRepository<CourseTeacher, Long> {

    boolean existsByCourseIdAndTeacherId(Long courseId, Long teacherId);

    Optional<CourseTeacher> findByCourseIdAndTeacherId(Long courseId, Long teacherId);

    /** Danh sách giáo viên được giao của một khóa, kèm thông tin giáo viên (tránh N+1). */
    @Query("""
            SELECT ct FROM CourseTeacher ct
            JOIN FETCH ct.teacher t
            WHERE ct.course.id = :courseId
            ORDER BY ct.createdAt ASC
            """)
    List<CourseTeacher> findByCourseIdWithTeacher(@Param("courseId") Long courseId);

    /** ID giáo viên (chưa xóa tài khoản) được giao của một khóa — dùng để gửi thông báo. */
    @Query("""
            SELECT ct.teacher.id FROM CourseTeacher ct
            WHERE ct.course.id = :courseId AND ct.teacher.deleted = false
            """)
    List<Long> findTeacherIdsByCourseId(@Param("courseId") Long courseId);
}
