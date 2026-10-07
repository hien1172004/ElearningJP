package com.jp.elearningjp.dto.response.curriculum;

import com.jp.elearningjp.shared.enums.ProgressStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuizSubmitResponse {

    Integer totalQuestions;
    Integer correctCount;
    BigDecimal scorePercent;         // Điểm số 0 - 100%
    Integer passingScorePercent;     // Điểm chuẩn đạt (80%)
    boolean isPassed;                // Đạt hay trượt
    ProgressStatus lessonStatus;     // COMPLETED nếu passed
    BigDecimal bestScore;            // Điểm cao nhất sau lần thi này

    List<QuizDetailItemResponse> details; // Chi tiết từng câu kèm đáp án đúng và giải thích
}
