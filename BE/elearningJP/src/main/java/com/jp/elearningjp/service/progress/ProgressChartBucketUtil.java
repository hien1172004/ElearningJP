package com.jp.elearningjp.service.progress;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.List;

/**
 * Gom bucket thời gian cho biểu đồ tiến độ (Khoi 3).
 *
 * <p><b>Vì sao gộp ở BE mà không để FE tự gộp?</b> Quy tắc "tuần ISO 8601"
 * cộng với múi giờ nghiệp vụ rất dễ lệch 1 ngày giữa 2 phía. Khi gộp ở BE,
 * cột "tuần" của biểu đồ luôn khớp với tuần của bảng xếp hạng (Khoi 5) —
 * cùng một nguồn sự thật là {@link LeaderboardWeekUtil}.</p>
 *
 * <p>3 đơn vị bucket:</p>
 * <ul>
 *   <li>{@code day} — mỗi ngày 1 bucket</li>
 *   <li>{@code week} — tuần ISO 8601 (thứ 2 &rarr; chủ nhật)</li>
 *   <li>{@code month} — thang lich (ngay 1 &rarr; het thang)</li>
 * </ul>
 */
public final class ProgressChartBucketUtil {

    /** Bucket = 1 ngay. */
    public static final String GRANULARITY_DAY = "day";
    /** Bucket = 1 tuan ISO 8601 (thu 2 &rarr; chu nhat). */
    public static final String GRANULARITY_WEEK = "week";
    /** Bucket = 1 thang lich. */
    public static final String GRANULARITY_MONTH = "month";

    private ProgressChartBucketUtil() {
    }

    /**
     * Chuan hoa tham so granularity — sai / null &rarr; {@code day}.
     *
     * @param granularity gia tri tho tu client
     * @return {@code day} | {@code week} | {@code month}
     */
    public static String resolveGranularity(String granularity) {
        if (granularity == null) {
            return GRANULARITY_DAY;
        }
        String g = granularity.trim().toLowerCase();
        if (GRANULARITY_WEEK.equals(g) || GRANULARITY_MONTH.equals(g)) {
            return g;
        }
        return GRANULARITY_DAY;
    }

    /**
     * Ngay dau cua bucket chua {@code date}.
     *
     * @param date        ngay bat ky trong bucket
     * @param granularity {@code day} | {@code week} | {@code month}
     * @return ngay dau bucket
     */
    public static LocalDate bucketStartOf(LocalDate date, String granularity) {
        switch (granularity) {
            case GRANULARITY_WEEK:
                // Tai dung LeaderboardWeekUtil de neo tuan ISO 8601 — cung nguon
                // voi bang xep hang tuan (Khoi 5).
                String weekKey = LeaderboardWeekUtil.format(
                        date.get(IsoFields.WEEK_BASED_YEAR),
                        date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
                return LeaderboardWeekUtil.weekStart(weekKey);
            case GRANULARITY_MONTH:
                return YearMonth.from(date).atDay(1);
            case GRANULARITY_DAY:
            default:
                return date;
        }
    }

    /**
     * Dung danh sach bucket tu {@code start} den {@code end} (inclusive).
     *
     * <p>Danh sach luon <b>day du</b> — ke ca cac bucket khong co hoat dong —
     * de duong "tich luy" khong bi dut khoang.</p>
     *
     * @param start       ngay dau ky
     * @param end         ngay cuoi ky
     * @param granularity {@code day} | {@code week} | {@code month}
     * @return danh sach ngay dau bucket, tang dan
     */
    public static List<LocalDate> buildBuckets(LocalDate start, LocalDate end, String granularity) {
        List<LocalDate> buckets = new ArrayList<>();
        if (start == null || end == null || start.isAfter(end)) {
            return buckets;
        }

        switch (granularity) {
            case GRANULARITY_WEEK: {
                LocalDate cursor = bucketStartOf(start, granularity);
                while (!cursor.isAfter(end)) {
                    buckets.add(cursor);
                    cursor = cursor.plusWeeks(1);
                }
                break;
            }
            case GRANULARITY_MONTH: {
                YearMonth ym = YearMonth.from(start);
                while (!ym.atDay(1).isAfter(end)) {
                    buckets.add(ym.atDay(1));
                    ym = ym.plusMonths(1);
                }
                break;
            }
            case GRANULARITY_DAY:
            default: {
                for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                    buckets.add(d);
                }
                break;
            }
        }
        return buckets;
    }
}
