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
    Long selectedOptionId;
    String selectedOptionText;
    Long correctOptionId;
    String correctOptionText;
    boolean isCorrect;
    String explanation;
}
