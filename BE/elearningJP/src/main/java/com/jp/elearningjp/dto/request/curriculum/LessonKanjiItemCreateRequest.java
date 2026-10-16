package com.jp.elearningjp.dto.request.curriculum;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
 * Request body để thêm 1 chữ Hán vào section KANJI.
 *
 * <p>Khi tạo, giáo viên/admin có thể đính kèm luồng danh sách
 * {@link LessonKanjiRelatedVocabRequest relatedVocabulary} (giáo viên chọn từ từ điển
 * hoặc tạo mới vocab rồi gắn vào). Nếu không truyền → danh sách rỗng.</p>
 *
 * <p>Ví dụ:</p>
 * <pre>{@code
 * {
 *   "character": "学",
 *   "orderIndex": 1,
 *   "note": "Chữ Hán đầu tiên trong bài",
 *   "relatedVocabulary": [
 *     { "vocabId": 12 },
 *     { "newVocab": { "word": "学生", "hiragana": "がくせい", "meaningVi": "học sinh" } }
 *   ]
 * }
 * }</pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonKanjiItemCreateRequest {

    @NotBlank(message = "Ký tự chữ Hán không được để trống")
    @Size(min = 1, max = 8, message = "Mỗi chữ Hán là 1 ký tự, tối đa 8 chữ")
    @Schema(description = "Chữ Hán (1 hoặc nhiều ký tự) - sẽ được validate qua từ điển",
            example = "学", requiredMode = Schema.RequiredMode.REQUIRED)
    String character;

    @Positive(message = "orderIndex phải > 0")
    @Schema(description = "Thứ tự hiển thị trong section (bỏ qua nếu muốn tự động gán)", example = "1")
    Integer orderIndex;

    @Size(max = 2000, message = "Ghi chú tối đa 2000 ký tự")
    @Schema(description = "Ghi chú của giáo viên cho chữ Hán này (tùy chọn)")
    String note;

    @Valid
    @Schema(description = "Danh sách related vocab do giáo viên chọn (từ từ điển hoặc tạo mới). "
            + "Tối đa 50 entry mỗi lần tạo. Bỏ trống nếu không muốn.")
    List<LessonKanjiRelatedVocabRequest> relatedVocabulary;
}