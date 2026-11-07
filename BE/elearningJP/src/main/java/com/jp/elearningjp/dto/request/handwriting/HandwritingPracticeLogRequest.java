package com.jp.elearningjp.dto.request.handwriting;

import com.fasterxml.jackson.databind.JsonNode;
import com.jp.elearningjp.entity.handwriting.HandwritingInputMode;
import com.jp.elearningjp.entity.handwriting.HandwritingPracticeMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Kết quả một lần luyện viết Kanji (FE gửi sau khi chấm điểm xong)")
public class HandwritingPracticeLogRequest {

    @Schema(description = "Bài học đang luyện; bỏ trống nếu luyện ở từ điển / sổ tay", example = "12")
    private Long lessonId;

    @NotBlank
    @Size(max = 8)
    @Schema(description = "Ký tự đã luyện", example = "見")
    private String character;

    @NotNull
    @Schema(description = "GUIDED = Hanzi-Writer, FREEHAND = viết tự do + AI chấm", example = "FREEHAND")
    private HandwritingPracticeMode practiceMode;

    @NotNull
    @Schema(description = "MOUSE = chuột/cảm ứng, AIR = viết bằng tay qua camera", example = "AIR")
    private HandwritingInputMode inputMode;

    @NotNull
    @DecimalMin("0")
    @DecimalMax("100")
    @Schema(description = "Điểm tổng 0-100", example = "86.5")
    private BigDecimal totalScore;

    @Min(0)
    @Max(100)
    @Schema(description = "Số nét chuẩn của chữ", example = "7")
    private Integer strokeCountExpected;

    @Min(0)
    @Max(200)
    @Schema(description = "Số nét người học đã vẽ (chế độ FREEHAND)", example = "7")
    private Integer strokeCountDrawn;

    @Min(0)
    @Max(1000)
    @Schema(description = "Số lần vẽ sai nét (chế độ GUIDED)", example = "2")
    private Integer totalMistakes;

    @Schema(description = "Chi tiết chấm từng nét (stroke_details từ AI), không bắt buộc")
    private JsonNode detail;
}
