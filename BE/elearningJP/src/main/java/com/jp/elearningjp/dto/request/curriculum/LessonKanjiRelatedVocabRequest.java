package com.jp.elearningjp.dto.request.curriculum;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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
 * Request body cho 1 related vocab của {@link com.jp.elearningjp.entity.curriculum.LessonKanjiItem}.
 *
 * <p>Mỗi entry có thể chứa ĐỒNG THỜI:</p>
 * <ul>
 *   <li>{@code vocabIds} (list, tuỳ chọn) — chọn nhiều vocab có sẵn trong từ điển theo id.</li>
 *   <li>{@code newVocab} (object, tuỳ chọn) — tạo 1 vocab mới trong từ điển rồi gắn vào.
 *       Bắt buộc phải có {@code word} (chữ viết) và {@code hiragana} (cách đọc).</li>
 * </ul>
 *
 * <p>Tối thiểu 1 trong 2 trường trên phải có giá trị. Service sẽ tạo {@code N} entity
 * {@code LessonKanjiRelatedVocab} từ 1 entry: tương ứng với {@code vocabIds.size() +
 * (newVocab != null ? 1 : 0)} bản ghi.</p>
 *
 * <p>Ví dụ — chọn 2 vocab có sẵn + tạo 1 vocab mới:</p>
 * <pre>{@code
 * {
 *   "vocabIds": [6, 7],
 *   "newVocab": {
 *     "word": "学生",
 *     "hiragana": "がくせい",
 *     "meaningVi": "học sinh"
 *   },
 *   "note": "3 vocab liên quan đến chữ 入"
 * }
 * }</pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonKanjiRelatedVocabRequest {

    @Schema(description = "Danh sách id của vocab có sẵn trong từ điển sẽ được gắn vào related vocab. "
            + "Mỗi id tạo ra 1 bản ghi lesson_kanji_related_vocabulary. "
            + "Bỏ trống hoặc null nếu không chọn vocab có sẵn.",
            example = "[6, 7]")
    List<@Positive(message = "Mỗi vocabId phải > 0") Long> vocabIds;

    @Valid
    @Schema(description = "Tạo mới 1 vocab trong từ điển rồi gắn vào related vocab. "
            + "Dùng khi giáo viên muốn thêm từ vựng mới chưa có trong từ điển. "
            + "Bỏ trống hoặc null nếu không tạo vocab mới.")
    NewVocabPayload newVocab;

    @Size(max = 2000, message = "Ghi chú tối đa 2000 ký tự")
    @Schema(description = "Ghi chú của giáo viên (áp dụng cho cả danh sách vocab trong entry này, tùy chọn)")
    String note;

    /**
     * @return true nếu entry này chứa ít nhất 1 vocabId hoặc newVocab hợp lệ.
     */
    public boolean isValid() {
        boolean hasVocabIds = vocabIds != null && !vocabIds.isEmpty();
        boolean hasNewVocab = newVocab != null
                && newVocab.getWord() != null && !newVocab.getWord().isBlank()
                && newVocab.getHiragana() != null && !newVocab.getHiragana().isBlank();
        return hasVocabIds || hasNewVocab;
    }

    /**
     * @return số lượng vocab sẽ được tạo ra từ entry này (= vocabIds.size() + 1 nếu có newVocab).
     */
    public int countVocabs() {
        int count = 0;
        if (vocabIds != null) {
            for (Long id : vocabIds) {
                if (id != null && id > 0) count++;
            }
        }
        if (newVocab != null
                && newVocab.getWord() != null && !newVocab.getWord().isBlank()
                && newVocab.getHiragana() != null && !newVocab.getHiragana().isBlank()) {
            count++;
        }
        return count;
    }

    /**
     * Payload tối thiểu để tạo 1 vocab mới trong bảng {@code vocabulary}.
     * Sau khi tạo sẽ tự động được gắn vào lesson_kanji_related_vocabulary.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Schema(description = "Payload tạo vocab mới trong từ điển")
    public static class NewVocabPayload {

        @NotBlank(message = "word không được để trống")
        @Size(max = 100, message = "word tối đa 100 ký tự")
        @Schema(description = "Chữ viết của từ vựng (bắt buộc)", example = "学生", requiredMode = Schema.RequiredMode.REQUIRED)
        String word;

        @NotBlank(message = "hiragana không được để trống")
        @Size(max = 100, message = "hiragana tối đa 100 ký tự")
        @Schema(description = "Cách đọc hiragana (bắt buộc)", example = "がくせい", requiredMode = Schema.RequiredMode.REQUIRED)
        String hiragana;

        @Size(max = 150, message = "romaji tối đa 150 ký tự")
        @Schema(description = "Romaji (tùy chọn)", example = "gakusei")
        String romaji;

        @Size(max = 200, message = "hanViet tối đa 200 ký tự")
        @Schema(description = "Hán Việt (tùy chọn)", example = "học sinh")
        String hanViet;

        @Size(max = 5000, message = "meaningVi tối đa 5000 ký tự")
        @Schema(description = "Nghĩa tiếng Việt (tùy chọn)", example = "học sinh, người học")
        String meaningVi;

        @Size(max = 5000, message = "meaningEn tối đa 5000 ký tự")
        @Schema(description = "Nghĩa tiếng Anh (tùy chọn)", example = "student")
        String meaningEn;

        @Size(max = 50, message = "partOfSpeech tối đa 50 ký tự")
        @Schema(description = "Loại từ (tùy chọn)", example = "名詞 (danh từ)")
        String partOfSpeech;
    }
}