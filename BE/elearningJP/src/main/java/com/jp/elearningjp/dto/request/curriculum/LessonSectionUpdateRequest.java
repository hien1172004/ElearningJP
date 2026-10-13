package com.jp.elearningjp.dto.request.curriculum;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Request body để cập nhật 1 section đã có.
 * Tất cả field đều optional — chỉ gửi field muốn thay đổi.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonSectionUpdateRequest {

    @Size(max = 255, message = "Tiêu đề section tối đa 255 ký tự")
    @Schema(description = "Tiêu đề mới của section (bỏ qua nếu không đổi)", example = "Phần 1: Chữ Hán nâng cao")
    String title;

    @Positive(message = "orderIndex phải > 0")
    @Schema(description = "Thứ tự mới của section trong lesson (bỏ qua nếu không đổi)", example = "2")
    Integer orderIndex;
}
