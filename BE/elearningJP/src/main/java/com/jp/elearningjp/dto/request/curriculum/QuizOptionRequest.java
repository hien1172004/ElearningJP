package com.jp.elearningjp.dto.request.curriculum;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuizOptionRequest {

    @NotNull(message = "Số thứ tự lựa chọn không được để trống (1-4)")
    Short optionKey; // 1, 2, 3, 4 tương ứng A, B, C, D

    @NotBlank(message = "Nội dung lựa chọn không được để trống")
    String optionText;

    @Builder.Default
    boolean correct = false;
}
