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
 * Request body để thêm 1 điểm ngữ pháp vào section GRAMMAR.
 *
 * <p>Grammar item có thể kèm danh sách ví dụ (examples) do giáo viên tự cấu hình
 * (xem {@link LessonGrammarExampleCreateRequest}).</p>
 *
 * <p>Semantics cho trường {@code examples}:</p>
 * <ul>
 *   <li>{@code examples == null} (không gửi) HOẶC {@code examples = []} → auto-copy
 *       tất cả examples từ {@code grammarPoint.examplesJson} (cột JSONB) sang item.</li>
 *   <li>{@code examples = [...]} → dùng danh sách này làm examples của item
 *       (KHÔNG tự động copy từ từ điển). Teacher hoàn toàn kiểm soát.</li>
 * </ul>
 *
 * <p>Ví dụ — tạo item và auto-copy examples từ từ điển:</p>
 * <pre>{@code
 * {
 *   "grammarId": 7,
 *   "orderIndex": 1
 * }
 * }</pre>
 *
 * <p>Ví dụ — tạo item với 2 example do giáo viên cấu hình:</p>
 * <pre>{@code
 * {
 *   "grammarId": 7,
 *   "orderIndex": 1,
 *   "examples": [
 *     { "exampleJp": "私は学生です。", "exampleVi": "Tôi là học sinh.", "orderIndex": 1 },
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

    @Valid
    @Schema(description = "Danh sách ví dụ do giáo viên cấu hình cho item này. "
            + "Semantics: null hoặc [] = auto-copy từ grammarPoint.examplesJson; "
            + "[...] = dùng danh sách này (không auto-copy).")
    List<LessonGrammarExampleCreateRequest> examples;
}