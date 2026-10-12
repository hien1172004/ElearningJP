package com.jp.elearningjp.dto.request.curriculum;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
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
 * Request body để cập nhật 1 từ vựng trong section VOCABULARY.
 *
 * <p>Semantics cho các trường override (V23+):</p>
 * <ul>
 *   <li>Trường KHÔNG gửi (null) → giữ nguyên giá trị override hiện tại.</li>
 *   <li>Trường gửi = "" → set NULL (xoá override, hiển thị fallback về từ điển).</li>
 *   <li>Trường gửi giá trị → cập nhật override.</li>
 * </ul>
 *
 * <p>Semantics cho trường {@code examples} (3-state):</p>
 * <ul>
 *   <li>{@code examples == null} (không gửi) → giữ nguyên danh sách examples hiện tại của item.</li>
 *   <li>{@code examples = []} (mảng rỗng) → RESET: xoá tất cả examples hiện tại của item
 *       và auto-copy lại từ {@code vocabulary.examples} (lấy example tĩnh mới nhất từ từ điển).</li>
 *   <li>{@code examples = [...]} (có phần tử) → upsert từng example theo {@code id}
 *       (xem {@link LessonVocabularyExampleUpsertRequest}):
 *       <ul>
 *         <li>id = null → tạo mới</li>
 *         <li>id != null → cập nhật example hiện tại (404 nếu không tồn tại)</li>
 *       </ul>
 *       Các example hiện tại KHÔNG có trong list sẽ bị soft-delete.
 *   </li>
 * </ul>
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

    @Size(max = 150, message = "Romaji tùy chỉnh tối đa 150 ký tự")
    @Schema(description = "Ghi đè romaji (gửi null để xoá override)")
    String customRomaji;

    @Size(max = 500, message = "URL audio tùy chỉnh tối đa 500 ký tự")
    @Schema(description = "Ghi đè URL audio (gửi null để xoá override)")
    String customAudioUrl;

    @Schema(description = "Ghi đè nghĩa (gửi null để xoá override)")
    String customMeaning;

    @Size(max = 500, message = "URL ảnh tối đa 500 ký tự")
    @Schema(description = "URL ảnh mới (gửi null để xoá)")
    String imageUrl;

    @Size(max = 2000, message = "Ghi chú tối đa 2000 ký tự")
    @Schema(description = "Ghi chú mới")
    String note;

    @Valid
    @Schema(description = "Danh sách ví dụ do giáo viên cấu hình. "
            + "Semantics: null = giữ nguyên examples hiện tại; "
            + "[] = RESET về auto-copy từ vocabulary.examples; "
            + "[...] = upsert từng entry (id=null tạo mới, id!=null cập nhật; "
            + "các example hiện tại không có trong list sẽ bị soft-delete).")
    List<LessonVocabularyExampleUpsertRequest> examples;
}
