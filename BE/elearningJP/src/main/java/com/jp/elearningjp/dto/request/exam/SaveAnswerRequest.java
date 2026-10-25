package com.jp.elearningjp.dto.request.exam;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SaveAnswerRequest {
    @NotNull(message = "Question ID is required")
    Long questionId;

    Long selectedOptionId;

    List<Long> orderedOptionIds;

    Integer audioPlayCount;
}
