package com.jp.elearningjp.dto.response.speaking;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * Cấu trúc gợi ý sửa lỗi ngữ pháp / dùng từ từ AI.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GrammarCorrectionDto {

    private String original;

    private String fixed;

    @JsonProperty("note_vi")
    private String noteVi;
}
