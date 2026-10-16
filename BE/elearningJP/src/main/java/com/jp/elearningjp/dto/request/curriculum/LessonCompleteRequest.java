package com.jp.elearningjp.dto.request.curriculum;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonCompleteRequest {

    @DecimalMin(value = "0.00", message = "LESSON_SCORE_MIN")
    @DecimalMax(value = "100.00", message = "LESSON_SCORE_MAX")
    BigDecimal score;
}