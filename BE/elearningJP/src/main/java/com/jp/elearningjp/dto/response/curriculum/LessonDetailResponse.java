package com.jp.elearningjp.dto.response.curriculum;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jp.elearningjp.shared.enums.ProgressStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Chi tiết bài học — kèm sections và items bên trong.
 *
 * <p>SAU REFACTOR (V14): bỏ {@code lessonType}, {@code contentMarkdown},
 * {@code durationMinutes}.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LessonDetailResponse {

    Long id;
    Long courseId;
    String courseTitle;
    String title;
    Integer orderIndex;
    boolean hasQuiz;
    ProgressStatus status;
    BigDecimal bestScore;
    Instant lastAccessedAt;
    Instant completedAt;

    /**
     * Danh sách section của bài học.
     * Sắp xếp theo orderIndex. Mỗi section chứa items theo sectionType.
     */
    List<LessonSectionResponse> sections;
}
