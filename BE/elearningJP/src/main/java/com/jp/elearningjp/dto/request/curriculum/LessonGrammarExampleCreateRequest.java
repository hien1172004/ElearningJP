package com.jp.elearningjp.dto.request.curriculum;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Request body dùng trong {@link LessonGrammarItemCreateRequest#examples}
 * để tạo 1 ví dụ mới cho grammar item (KHÔNG CÓ {@code id} vì đây là CREATE).
 *
 * <p>So sánh với {@link LessonGrammarExampleUpsertRequest}:</p>
 * <ul>
 *   <li>Create (class này): KHÔNG có {@code id} — chỉ dùng khi tạo mới item,
 *       mọi entry đều là tạo mới example.</li>
 *   <li>Upsert: CÓ {@code id} — dùng khi cập nhật item, hỗ trợ cả tạo mới
 *       (id=null) lẫn cập nhật (id!=null).</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonGrammarExampleCreateRequest {

    @NotBlank(message = "Câu tiếng Nhật không được để trống")
    @Schema(description = "Câu ví dụ tiếng Nhật", example = "私は毎日日本語を勉強します。", requiredMode = Schema.RequiredMode.REQUIRED)
    String exampleJp;

    @NotBlank(message = "Bản dịch tiếng Việt không được để trống")
    @Schema(description = "Bản dịch tiếng Việt", example = "Tôi học tiếng Nhật mỗi ngày.", requiredMode = Schema.RequiredMode.REQUIRED)
    String exampleVi;

    @Positive(message = "orderIndex phải > 0")
    @Schema(description = "Thứ tự hiển thị (tùy chọn, tự gán nếu bỏ qua)", example = "1")
    Integer orderIndex;
}