package com.jp.elearningjp.dto.response.curriculum;

import com.jp.elearningjp.shared.enums.ProgressStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

/**
 * Outline bài học trong mục lục khóa học.
 *
 * <p>SAU REFACTOR (V14): bỏ {@code lessonType} và {@code durationMinutes}.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonOutlineResponse {

    Long id;
    /** Version để gửi lại khi sửa bài (optimistic locking). */
    Long version;
    String title;
    Integer orderIndex;
    boolean hasQuiz;
    boolean hasSpeaking;
    ProgressStatus status; // null nếu chưa đăng nhập hoặc chưa bắt đầu
    BigDecimal bestScore;  // null nếu chưa làm quiz
    Integer progressPercent; // % hoàn thành bài của học viên (COMPLETED = 100)
}
