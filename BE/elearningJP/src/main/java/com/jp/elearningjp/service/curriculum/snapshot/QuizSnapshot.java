package com.jp.elearningjp.service.curriculum.snapshot;

import com.fasterxml.jackson.databind.JsonNode;
import com.jp.elearningjp.shared.enums.QuizQuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Quiz của bài học ở dạng trung lập (kèm đáp án) — dùng cho cả snapshot và chấm điểm.
 * <b>Không trả thẳng ra client</b>: học viên chỉ nhận bản đã ẩn đáp án.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizSnapshot {

    private Long quizId;
    private String title;
    private String description;
    private Integer passingScorePercent;

    @Builder.Default
    private List<Question> questions = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Question {
        private Long id;
        private Long version;
        private String questionText;
        private Integer orderIndex;
        private QuizQuestionType questionType;
        private String explanation;
        private JsonNode matchingData;
        private String correctTextAnswer;
        /** Liên kết SRS (từ vựng / chữ Hán) của câu hỏi. */
        private Long vocabId;
        private Long characterId;

        @Builder.Default
        private List<Option> options = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Option {
        private Long id;
        private Short optionKey;
        private String optionText;
        private boolean correct;
        private String matchKey;
    }
}
