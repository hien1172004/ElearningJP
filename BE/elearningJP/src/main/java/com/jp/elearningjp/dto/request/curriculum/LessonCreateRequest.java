package com.jp.elearningjp.dto.request.curriculum;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * DTO tạo mới bài học.
 *
 * <p>SAU REFACTOR (V14): bỏ {@code lessonType}, {@code contentMarkdown},
 * {@code durationMinutes}. Toàn bộ nội dung bài học (Kanji / Vocabulary /
 * Grammar / Quiz) được thêm vào sau khi lesson đã tạo thông qua các
 * endpoint quản lý {@code LessonSection}.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonCreateRequest {

    @NotBlank(message = "Tiêu đề bài học không được để trống")
    @Size(max = 255, message = "Tiêu đề tối đa 255 ký tự")
    String title;

    @Min(value = 1, message = "Thứ tự tối thiểu là 1")
    Integer orderIndex; // optional — nếu null/<=0 sẽ tự gán max+1
}
