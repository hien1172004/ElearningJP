package com.jp.elearningjp.dto.response.curriculum;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuizQuestionResponse {

    Long id;
    String questionText;
    Integer orderIndex;
    List<QuizOptionResponse> options; // Danh sách 4 lựa chọn không có đáp án đúng
}
