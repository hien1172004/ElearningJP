package com.jp.elearningjp.dto.response.curriculum;

import com.fasterxml.jackson.databind.JsonNode;
import com.jp.elearningjp.shared.enums.QuizQuestionType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Map;

/**
 * Biến thể của {@link QuizQuestionResponse} <b>CÓ</b> kèm đáp án đúng.
 * Chỉ dành cho Admin/Teacher trong trang quản lý — KHÔNG trả cho học viên.
 *
 * <p>So với {@code QuizQuestionResponse}:</p>
 * <ul>
 *   <li>Có thêm {@code correctOptionId}, {@code correctOptionText} (MCQ).</li>
 *   <li>Có thêm {@code correct} (Boolean) cho MCQ — đáp án đúng option.</li>
 *   <li>Có thêm {@code matchKey} trong mỗi option (MATCHING — FE cần để vẽ
 *       đường ghép đúng khi review).</li>
 *   <li>Có thêm {@code correctTextAnswer} (FILL_IN_THE_BLANK) — bản rõ
 *       (BE vẫn ẩn ở {@code getLessonQuiz}).</li>
 *   <li>Có thêm {@code correctMatchingAnswers} (MATCHING) — map
 *       {@code slotKey -> correctText} do BE dựng từ options.</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuizQuestionWithAnswerResponse {

    Long id;
    String questionText;
    Integer orderIndex;
    QuizQuestionType questionType;
    String explanation;
    JsonNode matchingData;

    /** Đáp án đúng dạng text (FILL_IN_THE_BLANK). */
    String correctTextAnswer;

    /** Map slotKey -> correctText (MATCHING). */
    Map<String, String> correctMatchingAnswers;

    /**
     * Danh sách option <b>kèm</b> cờ {@code correct} + {@code matchKey}.
     * Lưu ý: khác với {@link QuizOptionResponse} ở chỗ có thêm 2 field này.
     */
    List<QuizOptionWithAnswer> options;

    // =========================================================================
    // Sub-type
    // =========================================================================
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class QuizOptionWithAnswer {
        Long id;
        Short optionKey;
        String optionText;
        boolean correct;
        String matchKey;
    }
}
