package com.jp.elearningjp.dto.response.curriculum;

import com.jp.elearningjp.shared.enums.EnrollmentStatus;
import com.jp.elearningjp.shared.enums.ProgressStatus;
import com.jp.elearningjp.shared.response.PageResponse;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.List;

/** Theo dõi học viên của một khóa học (trang giáo viên). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CourseStudentsResponse {

    Long courseId;
    String courseTitle;
    /** Số bài học (chưa xóa) của khóa — mẫu số của % tiến độ. */
    int totalLessons;
    Summary summary;
    PageResponse<StudentProgress> students;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Summary {
        long totalStudents;
        /** Đã hoàn thành mọi bài học. */
        long finishedStudents;
        /** Chưa mở bài nào. */
        long notStartedStudents;
        /** Có học trong {@link #recentDays} ngày gần đây. */
        long activeRecently;
        int recentDays;
        /** Tiến độ trung bình (0–100). */
        double avgProgressPercent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class StudentProgress {
        Long userId;
        String fullName;
        String email;
        String avatarUrl;
        EnrollmentStatus enrollmentStatus;
        Instant enrolledAt;
        int completedLessons;
        int inProgressLessons;
        int totalLessons;
        /** 0–100 */
        int progressPercent;
        Instant lastActiveAt;
        /** Điểm quiz trung bình (best_score các bài đã làm quiz); null nếu chưa làm. */
        Double avgQuizScore;
        /**
         * Điểm bài kiểm tra (exam) của khóa: trung bình điểm quy đổi cao nhất mỗi đề;
         * null nếu chưa thi. Chỉ có ở danh sách học viên.
         */
        Double avgExamScore;
    }

    /** Chi tiết tiến độ từng bài của một học viên. */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class StudentDetail {
        Long courseId;
        String courseTitle;
        StudentProgress student;
        List<LessonProgress> lessons;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class LessonProgress {
        Long lessonId;
        String title;
        Integer orderIndex;
        ProgressStatus status;
        Instant startedAt;
        Instant lastAccessedAt;
        Instant completedAt;
        Double bestScore;
    }
}
