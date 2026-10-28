package com.jp.elearningjp.service.progress;

import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.time.temporal.WeekFields;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Toàn bộ logic tính tuần ISO — gom về 1 chỗ duy nhất.
 *
 * <p><b>Chuẩn ISO 8601:</b> tuần luôn bắt đầu bằng <b>thứ 2</b> và kết thúc bằng
 * <b>chủ nhật</b>. Tuần đầu tiên của năm là tuần chứa ngày 4/1 (ngày đầu tiên
 * có ít nhất 4 ngày trong năm đó) → dùng {@link IsoFields#WEEK_BASED_YEAR} và
 * {@link IsoFields#WEEK_OF_WEEK_BASED_YEAR}.</p>
 *
 * <p><b>weekKey</b> có dạng {@code YYYY-Www} (VD {@code 2026-W42}), trong đó
 * {@code YYYY} là <b>week-based-year</b> — không nhất thiết trùng năm lịch.
 * VD tuần đầu tiên của năm 2021 có thể kéo dài từ 28/12/2020 → week-based-year
 * là 2020, không phải 2021.</p>
 *
 * <p><b>Lưu ý về bug đã sửa:</b> cách neo tuần vào {@code LocalDate.of(year, 1, 1)}
 * là sai vì ngày 1/1 có thể thuộc week-based-year của năm trước (VD {@code 2021-W01}
 * ra {@code 2019-12-30}). Cách đúng là neo vào {@code LocalDate.of(year, 1, 4)} —
 * ngày 4/1 luôn nằm trong ISO week 1 của week-based-year đó.</p>
 */
public final class LeaderboardWeekUtil {

    /** Định dạng weekKey: 4 chữ số năm + "-W" + 2 chữ số tuần. */
    private static final Pattern WEEK_KEY_PATTERN = Pattern.compile("^(\\d{4})-W(\\d{2})$");

    /** Số tuần ISO tối đa trong 1 năm. */
    private static final int MAX_WEEK_OF_YEAR = 53;

    private LeaderboardWeekUtil() {
    }

    // =====================================================================
    // Current week
    // =====================================================================

    /**
     * WeekKey của tuần hiện tại theo {@code clock} (múi giờ nghiệp vụ).
     *
     * @param clock clock nguồn của "hôm nay"
     * @return weekKey dạng {@code YYYY-Www}
     */
    public static String currentWeekKey(Clock clock) {
        LocalDate today = LocalDate.now(clock);
        return format(today.get(IsoFields.WEEK_BASED_YEAR), today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
    }

    // =====================================================================
    // Format / parse
    // =====================================================================

    /**
     * Format weekKey từ week-based-year + số tuần.
     */
    public static String format(int weekBasedYear, int weekOfWeekBasedYear) {
        return weekBasedYear + "-W" + String.format("%02d", weekOfWeekBasedYear);
    }

    /**
     * Parse & validate weekKey.
     *
     * <p>Chấp nhận {@code YYYY-W01} … {@code YYYY-W53}. Tuần không tồn tại trong
     * week-based-year đó (VD {@code 2021-W53} — năm 2021 chỉ có 52 tuần ISO)
     * vẫn được parse thành công vì phạm vi ngày của nó sẽ tự động trôi sang
     * tuần kế tiếp; tuy nhiên để an toàn ta dùng {@link #weekRange(String)} để
     * xác định ngày thực tế.</p>
     *
     * @param weekKey chuỗi cần parse
     * @return {@link WeekKey} đã validate
     * @throws AppException {@code LEADERBOARD_INVALID_WEEK} nếu sai định dạng
     */
    public static WeekKey parseWeekKey(String weekKey) {
        if (weekKey == null || weekKey.isBlank()) {
            throw new AppException(ErrorCode.LEADERBOARD_INVALID_WEEK);
        }
        Matcher m = WEEK_KEY_PATTERN.matcher(weekKey.trim());
        if (!m.matches()) {
            throw new AppException(ErrorCode.LEADERBOARD_INVALID_WEEK);
        }
        int year = Integer.parseInt(m.group(1));
        int week = Integer.parseInt(m.group(2));
        if (week < 1 || week > MAX_WEEK_OF_YEAR) {
            throw new AppException(ErrorCode.LEADERBOARD_INVALID_WEEK);
        }
        return new WeekKey(year, week);
    }

    // =====================================================================
    // Range
    // =====================================================================

    /**
     * Khoảng ngày của tuần ISO: {@code [thứ 2, chủ nhật]}.
     *
     * @param weekKey weekKey cần tính
     * @return mảng 2 phần tử: {@code [weekStart, weekEnd]}
     */
    public static LocalDate[] weekRange(String weekKey) {
        WeekKey parsed = parseWeekKey(weekKey);
        return weekRange(parsed);
    }

    /**
     * Khoảng ngày của tuần ISO từ {@link WeekKey}.
     *
     * <p>Cách tính: neo vào {@code LocalDate.of(weekBasedYear, 1, 4)} — ngày 4/1 luôn
     * thuộc ISO week 1 của week-based-year tương ứng → lấy {@code weekOfWeekBasedYear}
     * rồi dịch về thứ 2.</p>
     */
    public static LocalDate[] weekRange(WeekKey parsed) {
        LocalDate anchor = LocalDate.of(parsed.weekBasedYear(), 1, 4);
        LocalDate weekStart = anchor
                .with(IsoFields.WEEK_OF_WEEK_BASED_YEAR, parsed.weekOfWeekBasedYear())
                .with(WeekFields.ISO.dayOfWeek(), 1); // thứ 2
        return new LocalDate[]{weekStart, weekStart.plusDays(6)}; // → chủ nhật
    }

    /**
     * Ngày thứ 2 đầu tuần.
     */
    public static LocalDate weekStart(String weekKey) {
        return weekRange(weekKey)[0];
    }

    /**
     * Ngày chủ nhật cuối tuần.
     */
    public static LocalDate weekEnd(String weekKey) {
        return weekRange(weekKey)[1];
    }

    // =====================================================================
    // Shift
    // =====================================================================

    /**
     * Dịch weekKey {@code delta} tuần (âm = tuần trước, dương = tuần sau).
     *
     * <p>Dịch qua {@link #weekStart(String)} rồi tính lại week-based-year + số tuần
     * → xử lý đúng trường hợp giao năm (VD {@code 2020-W53} → {@code 2021-W01}).</p>
     *
     * @param weekKey tuần gốc
     * @param delta  số tuần cần dịch
     * @return weekKey mới
     */
    public static String shiftWeek(String weekKey, int delta) {
        LocalDate start = weekStart(weekKey);
        LocalDate shifted = start.plusWeeks(delta);
        return format(shifted.get(IsoFields.WEEK_BASED_YEAR),
                shifted.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
    }

    // =====================================================================
    // Validation
    // =====================================================================

    /**
     * Tuần này có nằm trong tương lai không (so với {@code clock}).
     *
     * @param weekKey tuần cần kiểm tra
     * @param clock   clock nguồn của "hôm nay"
     * @return {@code true} nếu tuần bắt đầu sau tuần hiện tại
     */
    public static boolean isFuture(String weekKey, Clock clock) {
        LocalDate start = weekStart(weekKey);
        LocalDate currentStart = weekStart(currentWeekKey(clock));
        return start.isAfter(currentStart);
    }

    /**
     * Tuần có còn trong thời gian lưu trữ của Redis không.
     *
     * <p>Tuần ngoài phạm vi lưu trữ sẽ đọc thẳng từ DB, <b>không</b> ghi lại Redis
     * (tránh hồi sinh key cũ đã hết hạn).</p>
     *
     * @param weekKey   tuần cần kiểm tra
     * @param clock     clock nguồn của "hôm nay"
     * @param retention số tuần được giữ lại (8 tuần = 56 ngày)
     * @return {@code true} nếu tuần còn trong retention
     */
    public static boolean isWithinRetention(String weekKey, Clock clock, int retention) {
        LocalDate currentStart = weekStart(currentWeekKey(clock));
        LocalDate targetStart = weekStart(weekKey);
        if (targetStart.isAfter(currentStart)) {
            return false;
        }
        long weeksAgo = (currentStart.toEpochDay() - targetStart.toEpochDay()) / 7;
        return weeksAgo < retention;
    }

    // =====================================================================
    // Records
    // =====================================================================

    /**
     * WeekKey đã parse thành công.
     *
     * @param weekBasedYear       năm theo week-based-year (không nhất thiết = năm lịch)
     * @param weekOfWeekBasedYear số tuần trong week-based-year (1..53)
     */
    public record WeekKey(int weekBasedYear, int weekOfWeekBasedYear) {
    }
}
