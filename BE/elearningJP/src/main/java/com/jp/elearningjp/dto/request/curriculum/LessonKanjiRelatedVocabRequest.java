package com.jp.elearningjp.dto.request.curriculum;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Request body để thêm 1 từ vựng liên quan cho chữ Hán.
 * Ví dụ: chữ Hán "学" → từ vựng liên quan: 学校, 学生, 大学
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonKanjiRelatedVocabRequest {

    @NotNull(message = "ID từ vựng trong từ điển không được để trống")
    @Schema(description = "ID của từ vựng liên quan trong bảng vocabulary", example = "42", requiredMode = Schema.RequiredMode.REQUIRED)
    Long vocabId;

    @Size(max = 2000, message = "Ghi chú tối đa 2000 ký tự")
    @Schema(description = "Ghi chú vì sao từ vựng này liên quan tới chữ Hán")
    String note;
}
