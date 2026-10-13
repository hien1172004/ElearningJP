package com.jp.elearningjp.dto.request.curriculum;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * DTO cập nhật bài học.
 *
 * <p>SAU REFACTOR (V14): bỏ {@code lessonType}, {@code contentMarkdown},
 * {@code durationMinutes}.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonUpdateRequest {

    @NotBlank(message = "Tiêu đề bài học không được để trống")
    @Size(max = 255, message = "Tiêu đề tối đa 255 ký tự")
    String title;

    @Min(value = 1, message = "Thứ tự tối thiểu là 1")
    Integer orderIndex;
}
