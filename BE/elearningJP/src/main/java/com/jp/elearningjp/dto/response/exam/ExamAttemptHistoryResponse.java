package com.jp.elearningjp.dto.response.exam;

import com.jp.elearningjp.shared.enums.AttemptStatus;
import com.jp.elearningjp.shared.enums.AttemptType;
import com.jp.elearningjp.shared.enums.JlptLevel;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExamAttemptHistoryResponse {
    Long attemptId;
    Long examId;
    String examTitle;
    JlptLevel jlptLevel;
    AttemptType attemptType;
    String sectionTitle;
    AttemptStatus status;
    Instant startedAt;
    Instant submittedAt;
    Integer timeSpentSeconds;
    BigDecimal totalRawScore;
    Integer totalScaledScore;
    Integer totalScaledMaxScore;
    Integer overallPassingScore;
    Boolean passed;
    List<AttemptGroupScoreResponse> groupScores;
}
