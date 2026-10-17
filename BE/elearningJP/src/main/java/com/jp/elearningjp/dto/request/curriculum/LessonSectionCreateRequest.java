package com.jp.elearningjp.dto.request.curriculum;

import com.jp.elearningjp.shared.enums.LessonSectionType;
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
 * Request body để tạo 1 section mới trong bài học.
 * Ví dụ:
 * <pre>{@code
 * {
 *   "sectionType": "KANJI",
 *   "title": "Phần 1: Chữ Hán cơ bản",
 *   "orderIndex": 2
 * }
 * }</pre>
 *
 * <p>Nếu {@code orderIndex} null hoặc {@code <= 0} → tự gán = MAX + 1 (append cuối).</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonSectionCreateRequest {

    @NotNull(message = "Loại section không được để trống")
    @Schema(description = "Loại section: KANJI / VOCABULARY / GRAMMAR (TEST đã bị loại bỏ — Quiz gắn trực tiếp với Lesson)", example = "KANJI", requiredMode = Schema.RequiredMode.REQUIRED)
    LessonSectionType sectionType;

    @Size(max = 255, message = "Tiêu đề section tối đa 255 ký tự")
    @Schema(description = "Tiêu đề hiển thị của section (tùy chọn)", example = "Phần 1: Chữ Hán cơ bản")
    String title;

    @Positive(message = "orderIndex phải > 0")
    @Schema(description = "Thứ tự hiển thị (tùy chọn; null = tự gán = max+1)", example = "2")
    Integer orderIndex;
}
