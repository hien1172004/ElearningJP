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
public class ReviewSectionResponse {
    Long id;
    SessionType sessionType;
    String sectionTitle;
    String audioUrl;
    Integer orderIndex;
    Integer durationMinutes;
    List<ReviewQuestionResponse> questions;
}
