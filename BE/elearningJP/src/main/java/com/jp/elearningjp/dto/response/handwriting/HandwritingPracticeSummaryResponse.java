package com.jp.elearningjp.dto.response.handwriting;

import com.jp.elearningjp.entity.handwriting.HandwritingInputMode;
import com.jp.elearningjp.entity.handwriting.HandwritingPracticeMode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Tổng hợp điểm luyện viết của user cho 1 chữ, tách theo chế độ + nguồn nhập")
public class HandwritingPracticeSummaryResponse {

    private String character;

    @Schema(description = "Bài học; null = luyện ngoài bài học")
    private Long lessonId;

    @Schema(description = "Tổng số lần luyện (mọi chế độ)")
    private long totalAttempts;

    @Schema(description = "Mỗi phần tử ứng với 1 cặp (practiceMode, inputMode) đã từng luyện")
    private List<ModeStat> stats;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModeStat {
        private HandwritingPracticeMode practiceMode;
        private HandwritingInputMode inputMode;
        private long attempts;
        private BigDecimal bestScore;
        private BigDecimal lastScore;
        private boolean passed;
        private Instant lastPracticedAt;
    }
}
