package com.jp.elearningjp.dto.response.exam;

import com.jp.elearningjp.shared.enums.SessionType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TakingSectionResponse {
    Long id;
    SessionType sessionType;
    String sectionTitle;
    String audioUrl;
    Integer orderIndex;
    Integer durationMinutes;
    List<TakingQuestionResponse> questions;
}
