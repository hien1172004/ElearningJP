package com.jp.elearningjp.dto.response.curriculum;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuizDetailItemResponse {

    Long questionId;
    String questionText;
    com.jp.elearningjp.shared.enums.QuizQuestionType questionType;

    // Chi tiết đáp án học viên chọn/gõ
    Long selectedOptionId;
    String selectedOptionText;
    String userTextAnswer; // Đáp án học viên tự gõ

    // Chi tiết đáp án đúng
    Long correctOptionId;
    String correctOptionText;
    String correctTextAnswer; // Đáp án đúng chuẩn

    // Chi tiết bổ sung cho dạng MATCHING
    com.fasterxml.jackson.databind.JsonNode matchingData;
    java.util.Map<String, String> userMatchingAnswers;    // Đáp án học viên ghép (slotKey -> userText)
    java.util.Map<String, String> correctMatchingAnswers; // Đáp án đúng chuẩn (slotKey -> correctText)

    boolean isCorrect;
    Double accuracyPercent; // Tỉ lệ chính xác (đặc biệt cho dạng MATCHING, ví dụ 80.0%)
    Integer responseTimeMs; // Thời gian làm câu hỏi này
    String explanation;
}
