package com.jp.elearningjp.dto.request.curriculum;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
 * Request body dùng trong {@link LessonVocabularyItemUpdateRequest#examples}
 * để upsert 1 ví dụ từ vựng.
 *
 * <ul>
 *   <li>{@code id} = null → tạo mới</li>
 *   <li>{@code id} != null → cập nhật bản ghi hiện tại (404 nếu không tồn tại)</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonVocabularyExampleUpsertRequest {

    @Schema(description = "ID của example hiện tại (null = tạo mới)", example = "200")
    Long id;

    @NotBlank(message = "Câu tiếng Nhật không được để trống")
    @Schema(description = "Câu ví dụ tiếng Nhật", example = "私は学生です。", requiredMode = Schema.RequiredMode.REQUIRED)
    String exampleJp;

    @NotBlank(message = "Bản dịch tiếng Việt không được để trống")
    @Schema(description = "Bản dịch tiếng Việt", example = "Tôi là học sinh.", requiredMode = Schema.RequiredMode.REQUIRED)
    String exampleVi;

    @Schema(description = "Phiên âm romaji (tùy chọn)", example = "Watashi wa gakusei desu.")
    String exampleRomaji;

    @Size(max = 500, message = "URL audio tối đa 500 ký tự")
    @Schema(description = "URL file audio mẫu (tùy chọn)")
    String audioUrl;

    @Positive(message = "orderIndex phải > 0")
    @Schema(description = "Thứ tự hiển thị (tùy chọn, tự gán nếu bỏ qua)", example = "1")
    Integer orderIndex;

    @Schema(description = "Đánh dấu là ví dụ chính (chỉ 1 ví dụ primary / từ vựng)", example = "true")
    Boolean isPrimary;
}
