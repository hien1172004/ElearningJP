package com.jp.elearningjp.dto.request.curriculum;

import com.jp.elearningjp.shared.enums.JlptLevel;
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
public class CourseUpdateRequest {

    @NotBlank(message = "Tiêu đề khóa học không được để trống")
    @Size(max = 255, message = "Tiêu đề tối đa 255 ký tự")
    String title;

    @NotNull(message = "Cấp độ JLPT không được để trống")
    JlptLevel jlptLevel;

    String description;

    @Size(max = 500, message = "Link ảnh thumbnail tối đa 500 ký tự")
    String thumbnailUrl;

    boolean published;
}
