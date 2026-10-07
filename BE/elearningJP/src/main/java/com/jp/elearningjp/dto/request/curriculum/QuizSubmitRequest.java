package com.jp.elearningjp.dto.request.curriculum;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuizSubmitRequest {

    @NotEmpty(message = "Danh sách câu trả lời không được rỗng")
    @Valid
    List<QuizAnswerItem> answers;
}
