package com.jp.elearningjp.dto.response.curriculum;

import com.jp.elearningjp.shared.enums.LessonType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonSummaryResponse {
    Long id;
    Long courseId;
    String title;
    LessonType lessonType;
    Integer orderIndex;
    Integer durationMinutes;
    Instant createdAt;
    Instant updatedAt;
}