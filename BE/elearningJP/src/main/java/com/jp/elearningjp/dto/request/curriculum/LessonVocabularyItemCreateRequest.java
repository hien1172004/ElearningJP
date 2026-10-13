package com.jp.elearningjp.dto.request.curriculum;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Request body để thêm 1 từ vựng vào section VOCABULARY.
 *
 * <p>Vocab item có thể kèm danh sách ví dụ (examples) do giáo viên tự cấu hình
 * (xem {@link LessonVocabularyExampleCreateRequest}).</p>
 *
 * <p>Semantics cho trường {@code examples}:</p>
 * <ul>
 *   <li>{@code examples == null} (không gửi) HOẶC {@code examples = []} → auto-copy
 *       tất cả examples (chưa xoá) từ {@code vocabulary.examples} sang item.</li>
 *   <li>{@code examples = [...]} → dùng danh sách này làm examples của item
 *       (KHÔNG tự động copy từ từ điển). Teacher hoàn toàn kiểm soát.</li>
 * </ul>
 *
 * <p>Ví dụ — tạo item và auto-copy examples từ từ điển:</p>
 * <pre>{@code
 * {
 *   "vocabId": 42,
 *   "orderIndex": 1
 * }
 * }</pre>
 *
 * <p>Ví dụ — tạo item với 2 example do giáo viên cấu hình:</p>
 * <pre>{@code
 * {
 *   "vocabId": 42,
 *   "orderIndex": 1,
 *   "examples": [
 *     { "exampleJp": "私は学生です。", "exampleVi": "Tôi là học sinh.", "isPrimary": true },
 *     { "exampleJp": "学生は毎日勉強します。", "exampleVi": "Học sinh học mỗi ngày.", "orderIndex": 2 }
 *   ]
 * }
 * }</pre>
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
    @Schema(description = "Ghi đè hiragana (nếu muốn dùng cách đọc khác với từ điển)", example = "がくせい")
    String customHiragana;

    @Size(max = 150, message = "Romaji tùy chỉnh tối đa 150 ký tự")
    @Schema(description = "Ghi đè romaji (nếu muốn dùng cách viết romaji khác với từ điển)",
            example = "gakusei")
    String customRomaji;

    @Size(max = 500, message = "URL audio tùy chỉnh tối đa 500 ký tự")
    @Schema(description = "Ghi đè URL audio (nếu muốn dùng audio riêng thay vì audio từ điển)")
    String customAudioUrl;

    @Schema(description = "Ghi đè nghĩa tiếng Việt cho bài học này")
    String customMeaning;

    @Size(max = 500, message = "URL ảnh tối đa 500 ký tự")
    @Schema(description = "URL ảnh minh họa riêng cho từ vựng trong bài học này")
    String imageUrl;

    @Size(max = 2000, message = "Ghi chú tối đa 2000 ký tự")
    @Schema(description = "Ghi chú của giáo viên")
    String note;

    @Valid
    @Schema(description = "Danh sách ví dụ do giáo viên cấu hình cho item này. "
            + "Semantics: null hoặc [] = auto-copy tất cả examples từ vocabulary.examples; "
            + "[...] = dùng danh sách này (không auto-copy).")
    List<LessonVocabularyExampleCreateRequest> examples;
}
