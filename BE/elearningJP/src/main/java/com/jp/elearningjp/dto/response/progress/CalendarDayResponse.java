package com.jp.elearningjp.dto.response.progress;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 1 ngày trong lịch học (Khoi 2 — Lich + streak).
 *
 * <p><b>Chỉ trả về ngày CÓ hoạt động.</b> Ngày không hoạt động không có row trong
 * {@code user_daily_stats} → vắng mặt khỏi {@code days}, FE tự điền các ô trống.</p>
 *
 * <p><b>Lưu ý về {@code goalMet}:</b> field này <b>KHÔNG</b> đọc thẳng cột
 * {@code user_daily_stats.goal_met}. Cột đó được tính theo quy tắc rộng hơn
 * ({@code lookups >= goal_words} HOẶC {@code lessons_completed > 0} HOẶC
 * {@code study_seconds >= min_study_seconds}). Trong lịch ta dùng quy tắc
 * hẹp {@code lookups >= goalWords} để khớp với thẻ "Mục tiêu ngày"
 * ({@code số từ đã tra / mục tiêu từ}) — nếu không, cùng một ngày có thể hiện
 * "đạt" ở lịch nhưng "chưa đạt" ở thẻ tổng hợp.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "1 ngày trong lịch học")
public class CalendarDayResponse {

    @Schema(description = "Ngày (inclusive)", example = "2026-10-15")
    private LocalDate date;

    @Schema(description = "Số lần tra từ điển trong ngày", example = "7")
    private Integer lookups;

    @Schema(description = "Số bài học hoàn thành trong ngày", example = "1")
    private Integer lessonsCompleted;

    @Schema(description = "Thời gian học trong ngày (giây)", example = "900")
    private Integer studySeconds;

    @Schema(description = "XP kiếm được trong ngày", example = "24")
    private Integer xpEarned;

    @Schema(description = "Ngày có học không (tra từ / học bài / học đủ thời gian)", example = "true")
    private Boolean hasActivity;

    @Schema(description = "Ngày có đạt mục tiêu từ không (lookups >= goalWords)", example = "true")
    private Boolean goalMet;
}