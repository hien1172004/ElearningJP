package com.jp.elearningjp.dto.request.curriculum;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Request giáo viên hoặc admin gán kịch bản hội thoại vào bài học.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonSpeakingAssignRequest {

    @NotNull(message = "ID kịch bản không được để trống")
    Long scenarioId;

    Integer orderIndex;

    @Min(value = 1, message = "Số lượt nói tối thiểu phải lớn hơn hoặc bằng 1")
    @Builder.Default
    Integer minTurns = 3;
}
