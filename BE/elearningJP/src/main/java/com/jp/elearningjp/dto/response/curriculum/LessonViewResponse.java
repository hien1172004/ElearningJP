package com.jp.elearningjp.dto.response.curriculum;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

/**
 * Response xem nhanh bài học (cho danh sách / preview).
 *
 * <p>SAU REFACTOR (V14): bỏ {@code lessonType}, {@code contentMarkdown},
 * {@code durationMinutes}.</p>
 */
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
    Integer orderIndex;
    Instant createdAt;
    Instant updatedAt;
}
