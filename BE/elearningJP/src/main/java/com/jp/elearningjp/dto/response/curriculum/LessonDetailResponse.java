package com.jp.elearningjp.dto.response.curriculum;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jp.elearningjp.shared.enums.LessonSectionType;
import com.jp.elearningjp.shared.enums.ProgressStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

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
    /** Version để gửi lại khi cập nhật (optimistic locking). */
    Long version;
    Long courseId;
    String courseTitle;
    String title;
    Integer orderIndex;
    boolean hasQuiz;
    boolean hasSpeaking;
    LessonSpeakingResponse speakingScenario;
    List<LessonSpeakingResponse> speakingScenarios;
    ProgressStatus status;
    BigDecimal bestScore;
    Instant lastAccessedAt;
    Instant completedAt;
    /** Số mục học viên đã xem theo loại phần (KANJI/VOCABULARY/GRAMMAR). */
    Map<LessonSectionType, Integer> sectionProgress;

    /**
     * Danh sách section của bài học.
     * Sắp xếp theo orderIndex. Mỗi section chứa items theo sectionType.
     */
    List<LessonSectionResponse> sections;
}
