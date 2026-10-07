package com.jp.elearningjp.dto.response.curriculum;

import com.jp.elearningjp.shared.enums.LessonType;
import com.jp.elearningjp.shared.enums.ProgressStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonOutlineResponse {

    Long id;
    String title;
    LessonType lessonType;
    Integer orderIndex;
    Integer durationMinutes;
    boolean hasQuiz;
    ProgressStatus status; // null nếu chưa đăng nhập hoặc chưa bắt đầu
    BigDecimal bestScore;  // null nếu chưa làm quiz
}
