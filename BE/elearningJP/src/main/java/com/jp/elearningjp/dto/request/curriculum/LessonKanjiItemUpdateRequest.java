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
 * Request body để cập nhật 1 chữ Hán đã thêm vào section.
 * Tất cả field đều optional.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonKanjiItemUpdateRequest {

    @Positive(message = "orderIndex phải > 0")
    @Schema(description = "Thứ tự mới trong section (bỏ qua nếu không đổi)", example = "2")
    Integer orderIndex;

    @Size(max = 2000, message = "Ghi chú tối đa 2000 ký tự")
    @Schema(description = "Ghi chú mới (bỏ qua nếu không đổi)")
    String note;
}
