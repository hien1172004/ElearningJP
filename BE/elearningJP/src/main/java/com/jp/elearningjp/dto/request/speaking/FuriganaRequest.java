package com.jp.elearningjp.dto.request.speaking;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FuriganaRequest {

    @NotBlank(message = "Nội dung tiếng Nhật không được để trống")
    private String text;
}
