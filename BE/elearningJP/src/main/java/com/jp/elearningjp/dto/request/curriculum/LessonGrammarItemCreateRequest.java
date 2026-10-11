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
 * Request body để thêm 1 điểm ngữ pháp vào section.
 * Giáo viên có thể override meaning/structure riêng.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonGrammarItemCreateRequest {

    @NotNull(message = "ID ngữ pháp trong từ điển không được để trống")
    @Schema(description = "ID của grammar point trong bảng grammar_points", example = "7", requiredMode = Schema.RequiredMode.REQUIRED)
    Long grammarId;

    @Positive(message = "orderIndex phải > 0")
    @Schema(description = "Thứ tự hiển thị (tùy chọn)", example = "1")
    Integer orderIndex;

    @Schema(description = "Ghi đè nghĩa tiếng Việt cho bài học này")
    String customMeaning;

    @Schema(description = "Ghi đè cấu trúc ngữ pháp")
    String customStructure;

    @Size(max = 2000, message = "Ghi chú tối đa 2000 ký tự")
    @Schema(description = "Ghi chú của giáo viên")
    String note;
}
