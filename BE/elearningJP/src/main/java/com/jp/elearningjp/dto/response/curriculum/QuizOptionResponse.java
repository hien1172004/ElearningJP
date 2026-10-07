package com.jp.elearningjp.dto.response.curriculum;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuizOptionResponse {

    Long id;
    Short optionKey;   // 1, 2, 3, 4
    String optionText; // Nội dung đáp án (ẩn isCorrect để chống hack)
}
