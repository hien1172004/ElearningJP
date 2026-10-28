package com.jp.elearningjp.dto.response.exam;

import com.jp.elearningjp.shared.enums.AttemptType;
import com.jp.elearningjp.shared.enums.JlptLevel;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExamTakingResponse {
    Long attemptId;
    Long examId;
    String examTitle;
    JlptLevel jlptLevel;
    AttemptType attemptType;
    Long sectionId;
    Integer targetDurationMinutes;
    Instant startedAt;
    List<TakingSectionResponse> sections;
}
