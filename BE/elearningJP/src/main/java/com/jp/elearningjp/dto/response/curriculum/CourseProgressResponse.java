package com.jp.elearningjp.dto.response.curriculum;

import com.jp.elearningjp.shared.enums.JlptLevel;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
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
}