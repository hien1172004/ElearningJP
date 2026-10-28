package com.jp.elearningjp.dto.response.progress;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

/**
 * Lịch học + chuỗi streak theo tháng (Khoi 2).
 *
 * <p><b>Wrapper vì cần 3 thông tin mà {@code List} không mang được:</b></p>
 * <ol>
 *   <li>{@code goalWords} — mục tiêu từ/ngày, FE cần để chấm màu ô lịch và
 *       vẽ thanh "Mục tiêu ngày" (số từ MỚI / mục tiêu).</li>
 *   <li>{@code stats} — tổng hợp cả tháng (số ngày học, số ngày đạt mục tiêu)
 *       để hiện ở header khối.</li>
 *   <li>Tháng đang xem + ngày hôm nay — để FE biết ô nào là "hôm nay"
 *       (viền đậm) và ẩn chấm điểm ở tương lai.</li>
 * </ol>
 *
 * <p>{@code days} chỉ chứa ngày <b>có hoạt động</b> — ngày trống FE tự điền.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Lịch học + streak theo tháng")
public class CalendarResponse {

    @Schema(description = "Tháng đang xem (yyyy-MM)", example = "2026-10")
    private String month;

    @Schema(description = "Ngày đầu tháng", example = "2026-10-01")
    private LocalDate monthStart;

    @Schema(description = "Ngày cuối tháng", example = "2026-10-31")
    private LocalDate monthEnd;

    @Schema(description = "Hôm nay theo múi giờ nghiệp vụ", example = "2026-10-23")
    private LocalDate today;

    @Schema(description = "Mục tiêu từ MỚI/ngày (mặc định 5)", example = "5")
    private Integer goalWords;

    /** Chuỗi ngày liên tiếp tính đến hôm nay (hoặc hôm qua nếu hôm nay chưa học). */
    @Schema(description = "Chuỗi ngày học liên tiếp hiện tại (chỉ tính ngày có từ MỚI)", example = "7")
    private Integer currentStreak;

    @Schema(description = "Chuỗi ngày học liên tiếp dài nhất (chỉ tính ngày có từ MỚI)", example = "21")
    private Integer longestStreak;

    @Schema(description = "Số ngày có hoạt động trong tháng", example = "12")
    private Integer activeDays;

    @Schema(description = "Số ngày đạt mục tiêu trong tháng", example = "8")
    private Integer goalMetDays;

    @Schema(description = "Tổng lần tra từ trong tháng (kể cả tra lại từ đã học)", example = "64")
    private Integer totalLookups;

    @Schema(description = "Chỉ các ngày CÓ hoạt động, tăng dần theo ngày")
    private List<CalendarDayResponse> days;
}