package com.jp.elearningjp.dto.response.exam;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReviewStudentAnswerResponse {
    Long selectedOptionId;
    List<Long> orderedOptionIds;
    boolean correct;
    BigDecimal pointsEarned;
    Integer audioPlayCount;
}
