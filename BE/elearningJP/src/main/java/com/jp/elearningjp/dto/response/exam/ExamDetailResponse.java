package com.jp.elearningjp.dto.response.exam;

import com.jp.elearningjp.shared.enums.ExamType;
import com.jp.elearningjp.shared.enums.JlptLevel;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExamDetailResponse {
    Long id;
    String title;
    ExamType examType;
    JlptLevel jlptLevel;
    Integer totalDurationMinutes;
    Integer totalScaledMaxScore;
    Integer overallPassingScore;
    Integer totalQuestions;
    boolean published;
    List<ExamScoreGroupResponse> scoreGroups;
    List<ExamSectionSummaryResponse> sections;
}
