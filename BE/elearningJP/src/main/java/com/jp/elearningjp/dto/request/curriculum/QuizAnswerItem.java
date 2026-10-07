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

    @NotNull(message = "ID phương án lựa chọn không được để trống")
    Long selectedOptionId;

    Integer responseTimeMs;
}
