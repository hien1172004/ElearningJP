package com.jp.elearningjp.dto.response.exam;

import com.jp.elearningjp.shared.enums.ScoreGroupCode;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AttemptGroupScoreResponse {
    Long scoreGroupId;
    ScoreGroupCode groupCode;
    Integer rawCorrectCount;
    Integer rawTotalCount;
    Integer scaledScore;
    Integer maxScaledScore;
    Integer minRequiredScore;
    boolean groupPassed;
}
