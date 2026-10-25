package com.jp.elearningjp.dto.response.exam;

import com.jp.elearningjp.shared.enums.SessionType;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExamSectionSummaryResponse {
    Long id;
    SessionType sessionType;
    String sectionTitle;
    Integer orderIndex;
    Integer durationMinutes;
    String audioUrl;
    int questionCount;
}
