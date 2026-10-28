package com.jp.elearningjp.dto.response.progress;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Tổng hợp tiến độ học tập cho 1 kỳ (Khoi 1 — Summary).
 *
 * <p>Định nghĩa nghiệp vụ (đã chốt):</p>
 * <ul>
 *   <li>{@code wordsLearned} = <b>số MỤC MỚI</b> đã học trong kỳ (mỗi mục chỉ tính 1 lần)</li>
 *   <li>{@code totalLookups} = <b>TỔNG số lần tra từ điển</b> trong kỳ (kể cả tra lại từ cũ)</li>
 *   <li>Streak = số ngày liên tiếp có {@code >= 1} mục MỚI (chính sách STRICT)</li>
 *   <li>Mục tiêu mặc định = 5 mục mới/ngày</li>
 * </ul>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Tổng hợp tiến độ học tập theo kỳ")
public class ProgressSummaryResponse {

    // ------------------------------------------------------------------
    // Khoảng thời gian
    // ------------------------------------------------------------------

    @Schema(description = "Ngày bắt đầu kỳ (inclusive)", example = "2026-10-01")
    private LocalDate from;

    @Schema(description = "Ngày kết thúc kỳ (inclusive)", example = "2026-10-31")
    private LocalDate to;

    // ------------------------------------------------------------------
    // Chỉ số chính (Khoi 1)
    // ------------------------------------------------------------------

    @Schema(description = "Số từ đã học = số MỤC MỚI học lần đầu trong kỳ", example = "128")
    private Long wordsLearned;

    @Schema(description = "Tổng số lần tra từ điển trong kỳ (kể cả tra lại từ đã học)", example = "312")
    private Long totalLookups;

    @Schema(description = "Tổng thời gian học trong kỳ (giây)", example = "14400")
    private Long studySeconds;

    @Schema(description = "Tổng XP kiếm được trong kỳ", example = "256")
    private Long xpEarned;

    @Schema(description = "Số ngày có hoạt động (học được ít nhất 1 mục mới, hoặc học bài / học đủ thời gian)", example = "20")
    private Integer activeDays;

    @Schema(description = "Số ngày đạt mục tiêu (đủ số mục mới theo mục tiêu)", example = "14")
    private Integer goalMetDays;

    // ------------------------------------------------------------------
    // Streak
    // ------------------------------------------------------------------

    @Schema(description = "Chuỗi ngày học liên tiếp hiện tại", example = "7")
    private Integer currentStreak;

    @Schema(description = "Chuỗi ngày học liên tiếp dài nhất từ trước đến nay", example = "21")
    private Integer longestStreak;

    // ------------------------------------------------------------------
    // So sánh với kỳ trước
    // ------------------------------------------------------------------

    @Schema(description = "Tỷ lệ thay đổi của wordsLearned so với kỳ trước cùng độ dài (%). null nếu kỳ trước = 0.",
            example = "12.5")
    private Double wordsLearnedDeltaPct;

    @Schema(description = "Tỷ lệ thay đổi của studySeconds so với kỳ trước (%)", example = "-3.0")
    private Double studySecondsDeltaPct;

    // ------------------------------------------------------------------
    // Mục tiêu
    // ------------------------------------------------------------------

    /**
     * Mục tiêu từ/ngày đang áp dụng
     */
    @Schema(description = "Mục tiêu từ/ngày đang áp dụng", example = "5")
    private Integer goalWords;

    // ------------------------------------------------------------------
    // Hôm nay (Khoi 1 — theo dõi mục tiêu ngay)
    // ------------------------------------------------------------------

    @Schema(description = "Hôm nay theo múi giờ nghiệp vụ (Asia/Ho_Chi_Minh)", example = "2026-10-23")
    private LocalDate today;

    @Schema(description = "Số từ đã học HÔM NAY (= số mục MỚI học lần đầu, không phụ thuộc kỳ đã chọn)", example = "3")
    private Integer wordsLearnedToday;

    @Schema(description = "Số lần tra từ điển HÔM NAY (kể cả tra lại từ đã học)", example = "9")
    private Integer lookupsToday;

    /**
     * Hôm nay có đạt mục tiêu không (theo quy tắc hẹp {@code mục mới >= goalWords}).
     *
     * <p>Không đọc cột {@code user_daily_stats.goal_met} vì cột đó còn tính cả
     * "học xong 1 bài" / "học đủ 5 phút" — sẽ lệch với lịch ở Khối 2.</p>
     */
    @Schema(description = "Hôm nay đã đạt mục tiêu từ chưa", example = "false")
    private Boolean goalMetToday;
}