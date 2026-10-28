package com.jp.elearningjp.dto.request.speaking;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendMessageRequest {

    @NotBlank(message = "Nội dung câu nói của người học không được để trống")
    private String message;
}
