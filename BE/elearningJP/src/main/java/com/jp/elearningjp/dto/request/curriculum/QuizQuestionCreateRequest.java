package com.jp.elearningjp.dto.request.curriculum;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.jp.elearningjp.shared.enums.QuizQuestionType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuizQuestionCreateRequest {

    @NotBlank(message = "Nội dung câu hỏi không được để trống")
    String questionText;

    String explanation;

    @Builder.Default
    Integer orderIndex = 1;

    @Builder.Default
    QuizQuestionType questionType = com.jp.elearningjp.shared.enums.QuizQuestionType.MULTIPLE_CHOICE;

    String correctTextAnswer; // Đáp án đúng nếu dạng FILL_IN_THE_BLANK

    JsonNode matchingData; // Cấu trúc ô trống / cột nối nếu dạng MATCHING

    Long vocabId;

    Long characterId;

    @Valid
    List<QuizOptionRequest> options; // Phương án lựa chọn (cho MULTIPLE_CHOICE hoặc MATCHING)
}
