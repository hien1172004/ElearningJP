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
 * Request body để thêm 1 từ vựng vào section.
 * Giáo viên có thể override hiragana/meaning/image riêng cho bài học này.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonVocabularyItemCreateRequest {

    @NotNull(message = "ID từ vựng trong từ điển không được để trống")
    @Schema(description = "ID của từ vựng trong bảng vocabulary", example = "42", requiredMode = Schema.RequiredMode.REQUIRED)
    Long vocabId;

    @Positive(message = "orderIndex phải > 0")
    @Schema(description = "Thứ tự hiển thị (tùy chọn, tự gán nếu bỏ qua)", example = "1")
    Integer orderIndex;

    @Size(max = 100, message = "Hiragana tùy chỉnh tối đa 100 ký tự")
    @Schema(description = "Ghi đè hiragana (nếu muốn dùng cách đọc khác)", example = "がくせい")
    String customHiragana;

    @Schema(description = "Ghi đè nghĩa tiếng Việt cho bài học này")
    String customMeaning;

    @Size(max = 500, message = "URL ảnh tối đa 500 ký tự")
    @Schema(description = "URL ảnh minh họa riêng cho từ vựng trong bài học này")
    String imageUrl;

    @Size(max = 2000, message = "Ghi chú tối đa 2000 ký tự")
    @Schema(description = "Ghi chú của giáo viên")
    String note;
}
