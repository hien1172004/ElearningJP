package com.jp.elearningjp.dto.response.curriculum;

import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.curriculum.UserLessonProgress;
import com.jp.elearningjp.entity.curriculum.Lesson;
import com.jp.elearningjp.shared.enums.JlptLevel;
import com.jp.elearningjp.shared.enums.ProgressStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CourseProgressResponse {

    Long courseId;
    String courseTitle;
    JlptLevel courseLevel;
    Integer totalLessons;
    Integer completedLessons;
    Integer inProgressLessons;
    Integer notStartedLessons;
    BigDecimal progressPercent;
    boolean isCompleted;
    Instant enrolledAt;       // Thời điểm bắt đầu học lesson đầu tiên
    Instant lastAccessedAt;   // Lần truy cập gần nhất
    List<ProgressResponse> progress;  // Chi tiết progress từng lesson

    // ============================================================
    // FACTORY METHODS — gom logic build response trùng lặp ở các service
    // ============================================================

    /**
     * Build response dạng tóm tắt (không kèm list progress, inProgressLessons, v.v.).
     * Dùng cho API: {@code CourseService.getCourseProgress}, {@code LessonService.completeLesson}.
     */
    public static CourseProgressResponse summary(Long courseId,
                                                  String courseTitle,
                                                  int totalLessons,
                                                  int completedLessons,
                                                  BigDecimal progressPercent) {
        return CourseProgressResponse.builder()
                .courseId(courseId)
                .courseTitle(courseTitle)
                .totalLessons(totalLessons)
                .completedLessons(completedLessons)
                .progressPercent(progressPercent)
                .isCompleted(totalLessons > 0 && completedLessons >= totalLessons)
                .build();
    }

    /**
     * Build response dạng đầy đủ (kèm list progress từng lesson + các thống kê phụ).
     * Dùng cho API: {@code ProgressService.getMyProgressForCourse}.
     *
     * @param percentResult kết quả trả về từ
     *        {@code UserLessonProgressRepository.calculateCourseProgressPercent(userId, courseId)}.
     *        Có thể null nếu user chưa có progress.
     */
    public static CourseProgressResponse detailed(Long courseId,
                                                  String courseTitle,
                                                  JlptLevel courseLevel,
                                                  int totalLessons,
                                                  long completed,
                                                  long inProgress,
                                                  Instant firstStartedAt,
                                                  Instant lastAccessedAt,
                                                  List<ProgressResponse> progressList,
                                                  Integer percentResult) {
        int notStarted = Math.max(0, totalLessons - (int) completed - (int) inProgress);
        BigDecimal percent = percentResult == null
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(percentResult).setScale(1, RoundingMode.HALF_UP);

        return CourseProgressResponse.builder()
                .courseId(courseId)
                .courseTitle(courseTitle)
                .courseLevel(courseLevel)
                .totalLessons(totalLessons)
                .completedLessons((int) completed)
                .inProgressLessons((int) inProgress)
                .notStartedLessons(notStarted)
                .progressPercent(percent)
                .isCompleted(totalLessons > 0 && completed >= totalLessons)
                .enrolledAt(firstStartedAt)
                .lastAccessedAt(lastAccessedAt)
                .progress(progressList)
                .build();
    }
}