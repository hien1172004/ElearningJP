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
 * Request body để cập nhật 1 chữ Hán trong section KANJI.
 * Tất cả field đều optional.
 *
 * <p>Semantics cho {@code relatedVocabulary} (V25+):</p>
 * <ul>
 *   <li>Trường KHÔNG gửi (null) trong request → giữ nguyên danh sách related vocab hiện tại.</li>
 *   <li>Trường gửi = [] (mảng rỗng) → xoá hết related vocab do giáo viên chọn.</li>
 *   <li>Trường gửi 1 mảng → thay thế hoàn toàn danh sách related vocab (sync).</li>
 * </ul>
 *
 * <p>Khi sync, mỗi entry có thể là:</p>
 * <ul>
 *   <li>{@code vocabId != null} → dùng vocab đã có trong từ điển.</li>
 *   <li>{@code newVocab != null} → tạo vocab mới trong từ điển rồi gắn vào.</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonKanjiItemUpdateRequest {

    @Positive(message = "orderIndex phải > 0")
    @Schema(description = "Thứ tự mới trong section (bỏ qua nếu không đổi)", example = "2")
    Integer orderIndex;

    @Size(max = 2000, message = "Ghi chú tối đa 2000 ký tự")
    @Schema(description = "Ghi chú mới (bỏ qua nếu không đổi)")
    String note;

    @Valid
    @Schema(description = "Related vocab do giáo viên chọn. Bỏ trống → giữ nguyên, [] → xoá hết.")
    List<LessonKanjiRelatedVocabRequest> relatedVocabulary;
}