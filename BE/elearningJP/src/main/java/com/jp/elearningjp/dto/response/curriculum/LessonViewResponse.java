package com.jp.elearningjp.dto.response.curriculum;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LessonViewResponse {
    Long id;
    Long courseId;
    String title;
    LessonType lessonType;
    Integer orderIndex;
    Integer durationMinutes;
    String contentMarkdown;   // null nếu user không có quyền xem
    Instant createdAt;
    Instant updatedAt;
}