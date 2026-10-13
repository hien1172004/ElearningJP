package com.jp.elearningjp.dto.response.curriculum;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

/**
 * Response cơ bản cho Lesson.
 *
 * <p>SAU REFACTOR (V14): bỏ {@code lessonType}, {@code contentMarkdown},
 * {@code durationMinutes}.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonResponse {
    Long id;
    Long courseId;
    String title;
    Integer orderIndex;
    Instant createdAt;
    Instant updatedAt;
}
