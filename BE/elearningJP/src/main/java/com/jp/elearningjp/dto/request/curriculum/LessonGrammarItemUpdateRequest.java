package com.jp.elearningjp.dto.request.curriculum;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonGrammarItemUpdateRequest {

    @Positive(message = "orderIndex phải > 0")
    @Schema(description = "Thứ tự mới (bỏ qua nếu không đổi)")
    Integer orderIndex;

    @Schema(description = "Ghi đè nghĩa (gửi null để xoá override)")
    String customMeaning;

    @Schema(description = "Ghi đè cấu trúc (gửi null để xoá override)")
    String customStructure;

    @Size(max = 2000, message = "Ghi chú tối đa 2000 ký tự")
    @Schema(description = "Ghi chú mới")
    String note;
}
