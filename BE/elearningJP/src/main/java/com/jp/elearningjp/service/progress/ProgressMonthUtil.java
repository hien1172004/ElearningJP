package com.jp.elearningjp.service.progress;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Parse & validate chuỗi tháng {@code yyyy-MM} cho lịch học (Khoi 2).
 *
 * <p>Tách riêng khỏi {@link LeaderboardWeekUtil} vì <b>tiêu chuẩn khác nhau</b>:
 * tuần ISO dùng week-based-year (tuần chứa ngày 4/1 là tuần 1), còn tháng
 * lịch thì đơn giản là tháng lịch — không có khái niệm "tuần giao năm".</p>
 */
public final class ProgressMonthUtil {

    /** Định dạng tháng: 4 chữ số năm + "-" + 2 chữ số tháng. */
    private static final java.util.regex.Pattern MONTH_PATTERN =
            java.util.regex.Pattern.compile("^(\\d{4})-(0[1-9]|1[0-2])$");

    private ProgressMonthUtil() {
    }

    /**
     * Tháng hiện tại theo múi giờ nghiệp vụ.
     *
     * @param clock clock nguồn của "hôm nay"
     * @return chuỗi {@code yyyy-MM}
     */
    public static String currentMonth(Clock clock) {
        return YearMonth.from(LocalDate.now(clock)).toString();
    }

    /**
     * Parse chuỗi {@code yyyy-MM}. Chuỗi rỗng / sai định dạng → tháng hiện tại.
     *
     * <p><b>Vì sao không throw lỗi?</b> Tháng là tham số tiện ích để lướt lịch;
     * người dùng gõ sai thì rơi về tháng hiện tại vẫn hữu ích hơn là báo lỗi
     * trắng trang. (Ngược lại {@code week} của leaderboard là dữ liệu bảng xếp
     * hạng nên sai phải báo lỗi — xem {@code LEADERBOARD_INVALID_WEEK}.)</p>
     *
     * @param month chuỗi cần parse (null/blank → tháng hiện tại)
     * @param clock clock nguồn của "hôm nay"
     * @return {@link YearMonth} đã resolve
     */
    public static YearMonth resolve(String month, Clock clock) {
        if (month == null || month.isBlank()) {
            return YearMonth.from(LocalDate.now(clock));
        }
        var m = MONTH_PATTERN.matcher(month.trim());
        if (!m.matches()) {
            return YearMonth.from(LocalDate.now(clock));
        }
        return YearMonth.of(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)));
    }
}