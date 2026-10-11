package com.jp.elearningjp.dto.request.curriculum;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Request body để thêm 1 chữ Hán vào section.
 * Ví dụ:
 * <pre>{@code
 * {
 *   "characterId": 12,
 *   "orderIndex": 1,
 *   "note": "Chữ Hán đầu tiên trong bài"
 * }
 * }</pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonKanjiItemCreateRequest {

    @NotNull(message = "ID chữ Hán trong từ điển không được để trống")
    @Schema(description = "ID của chữ Hán trong bảng writing_characters", example = "12", requiredMode = Schema.RequiredMode.REQUIRED)
    Long characterId;

    @Positive(message = "orderIndex phải > 0")
    @Schema(description = "Thứ tự hiển thị trong section (bỏ qua nếu muốn tự động gán)", example = "1")
    Integer orderIndex;

    @Size(max = 2000, message = "Ghi chú tối đa 2000 ký tự")
    @Schema(description = "Ghi chú của giáo viên cho chữ Hán này (tùy chọn)")
    String note;
}
