package com.jp.elearningjp.dto.request.speaking;

import com.jp.elearningjp.shared.enums.AiPracticeMode;
import com.jp.elearningjp.shared.enums.JlptLevel;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSessionRequest {

    @NotNull(message = "Chế độ luyện nói (mode) không được để trống")
    private AiPracticeMode mode; // SCENARIO hoặc FREE

    private Long scenarioId; // Bắt buộc nếu mode = SCENARIO

    private String topic; // Bắt buộc nếu mode = FREE (ví dụ: "私の週末", "好きなアニメ")

    private JlptLevel jlptLevel; // Trình độ (N5 - N1), mặc định theo scenario hoặc N5 nếu FREE
}
