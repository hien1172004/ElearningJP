package com.jp.elearningjp.dto.response.curriculum;

import com.jp.elearningjp.shared.enums.LessonType;
import com.jp.elearningjp.shared.enums.ProgressStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonDetailResponse {

    Long id;
    Long courseId;
    String courseTitle;
    String title;
    LessonType lessonType;
    Integer orderIndex;
    Integer durationMinutes;
    String contentMarkdown; // Đầy đủ nội dung bài giảng
    boolean hasQuiz;
    ProgressStatus status;
    BigDecimal bestScore;
    Instant lastAccessedAt;
    Instant completedAt;
}
