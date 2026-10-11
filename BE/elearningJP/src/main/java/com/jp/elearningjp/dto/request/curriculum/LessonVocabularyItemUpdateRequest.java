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
 * Request body để cập nhật 1 từ vựng đã thêm vào section.
 * Tất cả field đều optional.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonVocabularyItemUpdateRequest {

    @Positive(message = "orderIndex phải > 0")
    @Schema(description = "Thứ tự mới (bỏ qua nếu không đổi)")
    Integer orderIndex;

    @Size(max = 100, message = "Hiragana tùy chỉnh tối đa 100 ký tự")
    @Schema(description = "Ghi đè hiragana (gửi null để xoá override)")
    String customHiragana;

    @Schema(description = "Ghi đè nghĩa (gửi null để xoá override)")
    String customMeaning;

    @Size(max = 500, message = "URL ảnh tối đa 500 ký tự")
    @Schema(description = "URL ảnh mới (gửi null để xoá)")
    String imageUrl;

    @Size(max = 2000, message = "Ghi chú tối đa 2000 ký tự")
    @Schema(description = "Ghi chú mới")
    String note;
}
