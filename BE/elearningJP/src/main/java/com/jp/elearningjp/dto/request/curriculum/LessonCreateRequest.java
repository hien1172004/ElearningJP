package com.jp.elearningjp.dto.request.curriculum;

import com.jp.elearningjp.shared.enums.LessonType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonCreateRequest {

    @NotBlank(message = "Tiêu đề bài học không được để trống")
    @Size(max = 255, message = "Tiêu đề tối đa 255 ký tự")
    String title;

    @NotNull(message = "Loại bài học không được để trống")
    LessonType lessonType;

    @NotNull(message = "Thứ tự sắp xếp không được để trống")
    @Min(value = 1, message = "Thứ tự tối thiểu là 1")
    Integer orderIndex;

    @Builder.Default
    @Min(value = 1, message = "Thời lượng tối thiểu 1 phút")
    Integer durationMinutes = 15;

    String contentMarkdown;
}
