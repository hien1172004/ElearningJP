package com.jp.elearningjp.dto.request.exam;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubmitExamRequest {
    List<SaveAnswerRequest> answers;
}
