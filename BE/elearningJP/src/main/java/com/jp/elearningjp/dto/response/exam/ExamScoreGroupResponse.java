package com.jp.elearningjp.dto.response.exam;

import com.jp.elearningjp.shared.enums.ScoreGroupCode;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExamScoreGroupResponse {
    Long id;
    ScoreGroupCode groupCode;
    Integer maxScaledScore;
    Integer minRequiredScore;
}
