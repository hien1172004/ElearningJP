package com.jp.elearningjp.dto.request.curriculum;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

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

    Long vocabId;

    Long characterId;

    @NotEmpty(message = "Phải có ít nhất 2 phương án lựa chọn")
    @Size(min = 2, max = 4, message = "Số lượng lựa chọn từ 2 đến 4 phương án")
    @Valid
    List<QuizOptionRequest> options;
}
