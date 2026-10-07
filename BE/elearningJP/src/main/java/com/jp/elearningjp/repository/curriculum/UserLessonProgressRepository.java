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
}
