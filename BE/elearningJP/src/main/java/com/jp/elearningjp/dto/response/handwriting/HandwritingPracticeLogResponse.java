package com.jp.elearningjp.dto.response.handwriting;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Kết quả lưu một lần luyện viết")
public class HandwritingPracticeLogResponse {

    private Long logId;

    @Schema(description = "Đạt (điểm >= ngưỡng đạt)")
    private boolean passed;

    @Schema(description = "Là điểm cao nhất mới của chữ này (cùng chế độ + nguồn nhập)")
    private boolean newBest;

    @Schema(description = "Tổng hợp sau khi lưu")
    private HandwritingPracticeSummaryResponse summary;
}
