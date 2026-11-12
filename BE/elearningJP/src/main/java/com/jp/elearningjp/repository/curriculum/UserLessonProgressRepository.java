package com.jp.elearningjp.repository.curriculum;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.Collection;
import java.time.Instant;
import org.springframework.data.jpa.repository.Modifying;
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

    /**
     * Bài tính vào tiến độ học viên: khóa đã có bản xuất bản → các bài trong bản đó
     * (kể cả bài vừa bị xóa ở bản đang soạn); chưa có → các bài chưa xóa như cũ.
     */
    String LEARNER_LESSON_L = "((l.course.publishedVersionId IS NULL AND l.deleted = false) "
            + "OR (l.course.publishedVersionId IS NOT NULL AND l.inPublishedVersion = true))";
    String LEARNER_LESSON_ULP = "((ulp.lesson.course.publishedVersionId IS NULL AND ulp.lesson.deleted = false) "
            + "OR (ulp.lesson.course.publishedVersionId IS NOT NULL AND ulp.lesson.inPublishedVersion = true))";

    Optional<UserLessonProgress> findByUserIdAndLessonIdAndDeletedFalse(Long userId, Long lessonId);

    Optional<UserLessonProgress> findByUser_IdAndLesson_IdAndDeletedFalse(Long userId, Long lessonId);

    @Query("SELECT COUNT(ulp) FROM UserLessonProgress ulp " +
           "WHERE ulp.user.id = :userId " +
           "AND ulp.lesson.course.id = :courseId " +
           "AND ulp.status = :status " +
           "AND " + LEARNER_LESSON_ULP + " " +
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
           "AND " + LEARNER_LESSON_L + " " +
           "AND ulp.deleted = false")
    List<UserLessonProgress> findByUserAndCourseWithLesson(@Param("userId") Long userId,
                                                           @Param("courseId") Long courseId);

    /**
     * Tính % tiến độ hoàn thành khóa học của user.
     * = (số lesson COMPLETED / tổng số lesson học viên đang học trong course) * 100
     * Trả về 0 nếu course chưa có lesson nào.
     */
    @Query(value = """
            SELECT CAST(
                CASE WHEN COUNT(l.id) = 0 THEN 0
                     ELSE (COUNT(CASE WHEN ulp.status = 'COMPLETED' THEN 1 END) * 100.0 / COUNT(l.id))
                END AS INTEGER)
            FROM lessons l
            JOIN courses c ON c.id = l.course_id
            LEFT JOIN user_lesson_progress ulp
                ON ulp.lesson_id = l.id
                AND ulp.user_id = :userId
                AND ulp.deleted = false
            WHERE l.course_id = :courseId
              AND ((c.published_version_id IS NULL AND l.deleted = false)
                   OR (c.published_version_id IS NOT NULL AND l.in_published_version = true))
            """, nativeQuery = true)
    Integer calculateCourseProgressPercent(@Param("userId") Long userId,
                                           @Param("courseId") Long courseId);

    /** Số bài active và số bài đã hoàn thành của user theo từng khóa. */
    @Query("""
            SELECT l.course.id AS courseId, COUNT(l.id) AS totalLessons, COUNT(ulp.id) AS completedLessons
            FROM Lesson l
            LEFT JOIN UserLessonProgress ulp
                ON ulp.lesson.id = l.id
                AND ulp.user.id = :userId
                AND ulp.status = :status
                AND ulp.deleted = false
            WHERE l.course.id IN :courseIds
              AND ((l.course.publishedVersionId IS NULL AND l.deleted = false)
                   OR (l.course.publishedVersionId IS NOT NULL AND l.inPublishedVersion = true))
            GROUP BY l.course.id
            """)
    List<CourseProgressCount> countCourseProgress(@Param("userId") Long userId,
                                                  @Param("courseIds") Collection<Long> courseIds,
                                                  @Param("status") ProgressStatus status);

    /**
     * Bản gộp của {@link #calculateCourseProgressPercent}, cùng cách làm tròn.
     * Khóa không có bài sẽ không có trong map.
     */
    default Map<Long, Integer> calculateCourseProgressPercents(Long userId, Collection<Long> courseIds) {
        return countCourseProgress(userId, courseIds, ProgressStatus.COMPLETED).stream()
                .collect(Collectors.toMap(
                        CourseProgressCount::getCourseId,
                        c -> (int) Math.round(c.getCompletedLessons() * 100.0 / c.getTotalLessons())));
    }

    @Modifying
    @Query("""
            UPDATE UserLessonProgress ulp SET ulp.deleted = true, ulp.deletedAt = :now
            WHERE ulp.lesson.id = :lessonId AND ulp.deleted = false
            """)
    int softDeleteByLessonId(@Param("lessonId") Long lessonId, @Param("now") Instant now);

    @Modifying
    @Query("""
            UPDATE UserLessonProgress ulp SET ulp.deleted = true, ulp.deletedAt = :now
            WHERE ulp.deleted = false
              AND ulp.lesson.id IN (SELECT l.id FROM Lesson l WHERE l.course.id = :courseId)
            """)
    int softDeleteByCourseId(@Param("courseId") Long courseId, @Param("now") Instant now);

    /**
     * Tạo dòng progress NOT_STARTED nếu user chưa có progress (chưa xoá) cho bài. Request đồng thời
     * chờ ở index uq_user_lesson_progress_active rồi bỏ qua thay vì vi phạm UNIQUE.
     *
     * @return 1 nếu vừa tạo, 0 nếu đã có
     */
    @Modifying
    @Query(value = """
            INSERT INTO user_lesson_progress (user_id, lesson_id, status)
            VALUES (:userId, :lessonId, 'NOT_STARTED')
            ON CONFLICT (user_id, lesson_id) WHERE deleted = false DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("userId") Long userId, @Param("lessonId") Long lessonId);

    /** Lấy progress (chưa xoá) và khoá dòng (SELECT ... FOR UPDATE) tới hết transaction. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ulp FROM UserLessonProgress ulp " +
           "WHERE ulp.user.id = :userId AND ulp.lesson.id = :lessonId AND ulp.deleted = false")
    Optional<UserLessonProgress> findActiveForUpdate(@Param("userId") Long userId,
                                                     @Param("lessonId") Long lessonId);

    interface CourseProgressCount {
        Long getCourseId();

        Long getTotalLessons();

        Long getCompletedLessons();
    }

    /** Học viên đã hoàn thành bài (tiến độ chưa xóa) — báo LESSON_UPDATED khi bài có thay đổi lớn. */
    @Query("SELECT DISTINCT p.user.id FROM UserLessonProgress p WHERE p.lesson.id = :lessonId "
            + "AND p.status = :status AND p.deleted = false")
    List<Long> findUserIdsByLessonIdAndStatus(@Param("lessonId") Long lessonId,
                                              @Param("status") ProgressStatus status);
}
