package com.jp.elearningjp.repository.curriculum;

import com.jp.elearningjp.entity.curriculum.UserLessonProgress;
import com.jp.elearningjp.shared.enums.ProgressStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserLessonProgressRepository extends JpaRepository<UserLessonProgress, Long> {

    Optional<UserLessonProgress> findByUserIdAndLessonIdAndDeletedFalse(Long userId, Long lessonId);

    Optional<UserLessonProgress> findByUser_IdAndLesson_IdAndDeletedFalse(Long userId, Long lessonId);

    @Query("SELECT COUNT(ulp) FROM UserLessonProgress ulp " +
           "WHERE ulp.user.id = :userId " +
           "AND ulp.lesson.course.id = :courseId " +
           "AND ulp.status = :status " +
           "AND ulp.deleted = false")
    long countCompletedLessonsByCourse(@Param("userId") Long userId,
                                      @Param("courseId") Long courseId,
                                      @Param("status") ProgressStatus status);

    @Query("SELECT ulp FROM UserLessonProgress ulp " +
           "WHERE ulp.user.id = :userId " +
           "AND ulp.lesson.course.id = :courseId " +
           "AND ulp.deleted = false")
    List<UserLessonProgress> findAllByUserIdAndCourseId(@Param("userId") Long userId,
                                                        @Param("courseId") Long courseId);

    /**
     * Lấy toàn bộ progress của user trong course, JOIN FETCH với lesson
     * để tránh N+1 query khi map sang response DTO.
     */
    @Query("SELECT ulp FROM UserLessonProgress ulp " +
           "JOIN FETCH ulp.lesson l " +
           "WHERE ulp.user.id = :userId " +
           "AND l.course.id = :courseId " +
           "AND ulp.deleted = false")
    List<UserLessonProgress> findByUserAndCourseWithLesson(@Param("userId") Long userId,
                                                           @Param("courseId") Long courseId);

    /**
     * Tính % tiến độ hoàn thành khóa học của user.
     * = (số lesson COMPLETED / tổng số lesson active trong course) * 100
     * Trả về 0 nếu course chưa có lesson nào.
     */
    @Query(value = """
            SELECT CAST(
                CASE WHEN COUNT(l.id) = 0 THEN 0
                     ELSE (COUNT(CASE WHEN ulp.status = 'COMPLETED' THEN 1 END) * 100.0 / COUNT(l.id))
                END AS INTEGER)
            FROM lessons l
            LEFT JOIN user_lesson_progress ulp
                ON ulp.lesson_id = l.id
                AND ulp.user_id = :userId
                AND ulp.deleted = false
            WHERE l.course_id = :courseId
              AND l.deleted = false
            """, nativeQuery = true)
    Integer calculateCourseProgressPercent(@Param("userId") Long userId,
                                           @Param("courseId") Long courseId);
}