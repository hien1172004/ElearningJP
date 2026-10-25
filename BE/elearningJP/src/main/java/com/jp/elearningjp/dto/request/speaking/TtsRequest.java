package com.jp.elearningjp.dto.request.speaking;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TtsRequest {

    @NotBlank(message = "Nội dung văn bản tiếng Nhật không được để trống")
    private String text;

    @Builder.Default
    private String voice = "ja-JP";

    private Long messageId; // Tùy chọn nếu muốn lưu audioUrl vào tin nhắn
}
