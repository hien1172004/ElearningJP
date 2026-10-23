package com.jp.elearningjp.service.progress;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test cho {@link ProgressMonthUtil} — resolve tháng cho lịch học.
 *
 * <p>Chú ý: dùng {@link Clock} cố định để test không phụ thuộc ngày chạy thật
 * (giống {@code LeaderboardWeekUtilTest}).</p>
 */
class ProgressMonthUtilTest {

    /** Múi giờ nghiệp vụ — phải khớp {@code TimeConfig.APP_ZONE}. */
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    /** 2026-10-23T12:00 theo GMT+7 → 05:00 UTC cùng ngày. */
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-10-23T05:00:00Z"), ZONE);

    @Nested
    @DisplayName("currentMonth")
    class CurrentMonth {

        @Test
        @DisplayName("trả về tháng hiện tại theo múi giờ nghiệp vụ")
        void returnsCurrentMonth() {
            assertThat(ProgressMonthUtil.currentMonth(FIXED_CLOCK)).isEqualTo("2026-10");
        }

        @Test
        @DisplayName("dùng múi giờ Asia/Ho_Chi_Minh, không phải UTC")
        void usesAppTimezoneNotUtc() {
            // 23/10 00:30 GMT+7 = 22/10 17:30 UTC → UTC sẽ ra tháng 10,
            // nhưng để chứng minh khác biệt ta dùng mốc sát ranh giới tháng.
            Clock nearMonthEnd = Clock.fixed(
                    Instant.parse("2026-10-31T17:00:00Z"), ZONE); // 01/11 00:00 GMT+7
            assertThat(ProgressMonthUtil.currentMonth(nearMonthEnd)).isEqualTo("2026-11");
        }
    }

    @Nested
    @DisplayName("resolve")
    class Resolve {

        @Test
        @DisplayName("parse tháng hợp lệ")
        void parsesValidMonth() {
            YearMonth ym = ProgressMonthUtil.resolve("2026-03", FIXED_CLOCK);
            assertThat(ym).isEqualTo(YearMonth.of(2026, 3));
        }

        @Test
        @DisplayName("null / rỗng → tháng hiện tại")
        void blankFallsBackToCurrent() {
            assertThat(ProgressMonthUtil.resolve(null, FIXED_CLOCK))
                    .isEqualTo(YearMonth.of(2026, 10));
            assertThat(ProgressMonthUtil.resolve("  ", FIXED_CLOCK))
                    .isEqualTo(YearMonth.of(2026, 10));
        }

        @Test
        @DisplayName("định dạng sai → tháng hiện tại (không throw)")
        void invalidFallsBackToCurrent() {
            // Sai định dạng / tháng không tồn tại → rơi về tháng hiện tại
            // (tháng là tham số tiện ích để lướt lịch, không phải dữ liệu chí mạng).
            assertThat(ProgressMonthUtil.resolve("2026-13", FIXED_CLOCK))
                    .isEqualTo(YearMonth.of(2026, 10));
            assertThat(ProgressMonthUtil.resolve("abc", FIXED_CLOCK))
                    .isEqualTo(YearMonth.of(2026, 10));
            assertThat(ProgressMonthUtil.resolve("2026-00", FIXED_CLOCK))
                    .isEqualTo(YearMonth.of(2026, 10));
        }

        @Test
        @DisplayName("bỏ khoảng trắng thừa quanh chuỗi")
        void trimsWhitespace() {
            assertThat(ProgressMonthUtil.resolve("  2026-07  ", FIXED_CLOCK))
                    .isEqualTo(YearMonth.of(2026, 7));
        }
    }

    @Nested
    @DisplayName("biên tháng")
    class MonthBoundaries {

        @Test
        @DisplayName("tháng 1 → tháng trước là tháng 12 năm trước")
        void januaryCrossesYear() {
            YearMonth ym = ProgressMonthUtil.resolve("2026-01", FIXED_CLOCK);
            assertThat(ym.minusMonths(1)).isEqualTo(YearMonth.of(2025, 12));
        }

        @Test
        @DisplayName("ngày đầu & cuối tháng")
        void monthStartAndEnd() {
            YearMonth ym = ProgressMonthUtil.resolve("2024-02", FIXED_CLOCK);
            assertThat(ym.atDay(1)).isEqualTo(LocalDate.of(2024, 2, 1));
            assertThat(ym.atEndOfMonth()).isEqualTo(LocalDate.of(2024, 2, 29));
        }
    }
}