package com.jp.elearningjp.dto.response.progress;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 1 điểm trên biểu đồ tiến độ (Khoi 3 — Bieu do time-series).
 *
 * <p><b>Điểm = 1 "bucket" (kỳ nhỏ)</b>, không nhất thiết = 1 ngày. Granularity
 * quyết định bucket là ngày / tuần ISO / tháng lịch (xem
 * {@code ChartResponse.granularity}).</p>
 *
 * <p>Vì sao không trả về từng ngày rồi để FE tự gộp? Vì quy tắc "tuần ISO 8601"
 * và múi giờ nghiệp vụ rất dễ lệch 1 ngày giữa BE và FE — gộp ở BE đảm bảo
 * cột "tuần" luôn khớp với bảng xếp hạng tuần (Khoi 5).</p>
 *
 * <p><b>Bucket không có hoạt động vẫn được trả về</b> với {@code wordsLearned = 0}
 * để đường "tích luỹ" không bị đứt khoảng — người dùng nhìn thấy rõ những
 * ngày học không đạt.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "1 diem tren bieu do tien do")
public class ChartPointResponse {

    @Schema(description = "Ngay bat dau cua bucket (inclusive)", example = "2026-10-15")
    private LocalDate bucketStart;

    @Schema(description = "So tu da tra trong bucket (0 neu khong hoc)", example = "7")
    private Integer wordsLearned;

    @Schema(description = "Tong so tu tich luy trong ky (cong don, bat dau tu 0)", example = "35")
    private Integer cumulativeWords;
}
