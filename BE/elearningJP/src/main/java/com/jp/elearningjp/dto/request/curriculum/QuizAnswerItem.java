package com.jp.elearningjp.dto.request.curriculum;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuizAnswerItem {

    @NotNull(message = "ID câu hỏi không được để trống")
    Long questionId;

    // Dùng cho dạng MULTIPLE_CHOICE
    Long selectedOptionId;

    // Dùng cho dạng FILL_IN_THE_BLANK (tự gõ đáp án)
    String textAnswer;

    // Dùng cho dạng MATCHING: Map giữa mã ô trống (targetKey) -> nội dung/key được kéo vào (selectedKey)
    java.util.Map<String, String> matchingAnswers;

    Integer responseTimeMs;
}
