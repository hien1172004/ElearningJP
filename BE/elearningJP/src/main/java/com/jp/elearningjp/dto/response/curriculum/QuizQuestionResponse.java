package com.jp.elearningjp.dto.response.curriculum;

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
public class QuizQuestionResponse {

    Long id;
    String questionText;
    Integer orderIndex;
    QuizQuestionType questionType;
    JsonNode matchingData; // Dữ liệu ô trống / câu nối (nếu là dạng MATCHING, đã ẩn đáp án đúng)
    List<QuizOptionResponse> options; // Danh sách các lựa chọn (cho MULTIPLE_CHOICE hoặc MATCHING)
}
