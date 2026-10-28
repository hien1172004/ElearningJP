package com.jp.elearningjp.dto.request.dictionary;

import com.jp.elearningjp.shared.enums.LookupTargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request ghi su kien "nguoi dung da tra tu 1 lan".
 *
 * <p>Dung cho endpoint {@code POST /api/v1/dictionary/lookup} (can auth).</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Ghi nhận 1 lần tra từ điển để cập nhật tiến độ học tập")
public class DictionaryLookupRequest {

    @NotNull
    @Schema(description = "Loại đối tượng được tra cứu", example = "VOCAB")
    private LookupTargetType targetType;

    @Schema(description = "ID của đối tượng (vocabulary.id / grammar_points.id). Null với KANJI",
            example = "123")
    private Long targetId;

    @Schema(description = "Chuỗi tham chiếu: kanji character, hoặc từ khóa đã tra",
            example = "日本語")
    @Size(max = 100)
    private String sourceRef;
}