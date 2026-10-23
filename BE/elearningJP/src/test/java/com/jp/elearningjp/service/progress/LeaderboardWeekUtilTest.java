package com.jp.elearningjp.service.progress;

import com.jp.elearningjp.exception.AppException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit test cho {@link LeaderboardWeekUtil} — logic tuần ISO.
 *
 * <p>Không cần Redis/DB vì đây là thuần phép tính lịch.</p>
 *
 * <p><b>Quan trọng:</b> test dùng {@link Clock} cố định để các mốc "hôm nay"
 * / "tuần này" không phụ thuộc ngày chạy thật — nếu không, test sẽ fail mỗi
 * khi chạy vào một ngày khác (hoặc khi qua ranh giới thứ 2).</p>
 */
class LeaderboardWeekUtilTest {

    /** Múi giờ nghiệp vụ — khớp `TimeConfig.APP_ZONE`. */
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    /** Clock cố định tại 2026-10-22 (thứ 5) — nằm trong tuần 2026-W43. */
    private static final Clock FIXED_CLOCK = Clock.fixed(
            LocalDate.of(2026, 10, 22).atStartOfDay(APP_ZONE).toInstant(), APP_ZONE);

    // =====================================================================
    // Tuần giao năm
    // =====================================================================

    @Nested
    @DisplayName("Tuần giao năm")
    class YearBoundary {

        @Test
        @DisplayName("2020-W53 là tuần 28/12/2020 - 03/01/2021")
        void week53Of2020() {
            LocalDate[] range = LeaderboardWeekUtil.weekRange("2020-W53");

            assertThat(range[0]).isEqualTo(LocalDate.of(2020, 12, 28)); // thứ 2
            assertThat(range[1]).isEqualTo(LocalDate.of(2021, 1, 3));   // chủ nhật
        }

        @Test
        @DisplayName("2021-W01 là tuần 04/01/2021 - 10/01/2021 (neo Jan 4, không phải Jan 1)")
        void week01Of2021() {
            LocalDate[] range = LeaderboardWeekUtil.weekRange("2021-W01");

            // Bug cũ: neo vào LocalDate.of(2021, 1, 1) → ra 2019-12-30.
            // Đúng phải là 2021-01-04 (ngày 4/1 luôn nằm trong ISO week 1).
            assertThat(range[0]).isEqualTo(LocalDate.of(2021, 1, 4));
            assertThat(range[1]).isEqualTo(LocalDate.of(2021, 1, 10));
        }

        @Test
        @DisplayName("shiftWeek qua giao năm: 2020-W53 + 1 tuần = 2021-W01")
        void shiftAcrossYearBoundary() {
            assertThat(LeaderboardWeekUtil.shiftWeek("2020-W53", 1)).isEqualTo("2021-W01");
            assertThat(LeaderboardWeekUtil.shiftWeek("2021-W01", -1)).isEqualTo("2020-W53");
        }

        @Test
        @DisplayName("shiftWeek nhiều tuần qua giao năm")
        void shiftMultipleWeeksAcrossYear() {
            assertThat(LeaderboardWeekUtil.shiftWeek("2020-W52", 2)).isEqualTo("2021-W01");
            assertThat(LeaderboardWeekUtil.shiftWeek("2021-W01", -2)).isEqualTo("2020-W52");
        }
    }

    // =====================================================================
    // Parse / format
    // =====================================================================

    @Nested
    @DisplayName("Parse & format")
    class Parsing {

        @Test
        @DisplayName("currentWeekKey dùng week-based-year")
        void currentWeekKey() {
            // 2026-10-22 là thứ 5 → tuần ISO 2026-W43
            assertThat(LeaderboardWeekUtil.currentWeekKey(FIXED_CLOCK)).isEqualTo("2026-W43");
        }

        @ParameterizedTest
        @ValueSource(strings = {"2026-W42", "2026-W01", "2026-W53", "2020-W53"})
        @DisplayName("Parse + format là round-trip")
        void roundTrip(String weekKey) {
            LeaderboardWeekUtil.WeekKey parsed = LeaderboardWeekUtil.parseWeekKey(weekKey);

            assertThat(LeaderboardWeekUtil.format(
                    parsed.weekBasedYear(), parsed.weekOfWeekBasedYear())).isEqualTo(weekKey);
        }

        @ParameterizedTest
        @ValueSource(strings = {"2026-W42", " 2026-W42  "})
        @DisplayName("Parse chấp nhận khoảng trắng thừa")
        void acceptsWhitespace(String input) {
            assertThat(LeaderboardWeekUtil.parseWeekKey(input).weekBasedYear()).isEqualTo(2026);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "26-W42",      // thiếu 1 chữ số năm
                "2026-W4",     // thiếu 1 chữ số tuần
                "2026-W54",    // tuần > 53
                "2026-W00",    // tuần < 1
                "2026-w42",    // sai hoa/thường
                "2026/42",     // sai dấu phân cách
                "2026-W42-extra",
                "abc",
                ""
        })
        @DisplayName("Parse tuần sai định dạng → LEADERBOARD_INVALID_WEEK")
        void rejectsInvalidFormat(String input) {
            assertThatThrownBy(() -> LeaderboardWeekUtil.parseWeekKey(input))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining("Invalid week key");
        }

        @Test
        @DisplayName("Parse null → LEADERBOARD_INVALID_WEEK")
        void rejectsNull() {
            assertThatThrownBy(() -> LeaderboardWeekUtil.parseWeekKey(null))
                    .isInstanceOf(AppException.class);
        }
    }

    // =====================================================================
    // Range
    // =====================================================================

    @Nested
    @DisplayName("Khoảng ngày")
    class Ranges {

        @ParameterizedTest
        @CsvSource({
                "2026-W42, 2026-10-12, 2026-10-18",
                "2026-W43, 2026-10-19, 2026-10-25",
                "2026-W01, 2025-12-29, 2026-01-04"
        })
        @DisplayName("weekRange trả đúng thứ 2 → chủ nhật")
        void weekRange(String weekKey, String expectedStart, String expectedEnd) {
            LocalDate[] range = LeaderboardWeekUtil.weekRange(weekKey);

            assertThat(range[0]).isEqualTo(LocalDate.parse(expectedStart));
            assertThat(range[1]).isEqualTo(LocalDate.parse(expectedEnd));
        }

        @Test
        @DisplayName("Tuần luôn kéo dài 7 ngày")
        void alwaysSevenDays() {
            LocalDate[] range = LeaderboardWeekUtil.weekRange("2026-W42");

            assertThat(range[0].getDayOfWeek().getValue()).isEqualTo(1); // thứ 2
            assertThat(range[1].getDayOfWeek().getValue()).isEqualTo(7); // chủ nhật
            assertThat(java.time.temporal.ChronoUnit.DAYS.between(range[0], range[1]))
                    .isEqualTo(6);
        }

        @Test
        @DisplayName("weekStart / weekEnd khớp weekRange")
        void accessorsMatchRange() {
            LocalDate[] range = LeaderboardWeekUtil.weekRange("2026-W42");

            assertThat(LeaderboardWeekUtil.weekStart("2026-W42")).isEqualTo(range[0]);
            assertThat(LeaderboardWeekUtil.weekEnd("2026-W42")).isEqualTo(range[1]);
        }
    }

    // =====================================================================
    // Validation
    // =====================================================================

    @Nested
    @DisplayName("Validate")
    class Validation {

        @Test
        @DisplayName("Tuần hiện tại không phải tương lai")
        void currentWeekIsNotFuture() {
            assertThat(LeaderboardWeekUtil.isFuture("2026-W43", FIXED_CLOCK)).isFalse();
        }

        @Test
        @DisplayName("Tuần sau là tương lai")
        void nextWeekIsFuture() {
            assertThat(LeaderboardWeekUtil.isFuture("2026-W44", FIXED_CLOCK)).isTrue();
        }

        @Test
        @DisplayName("Tuần trước không phải tương lai")
        void previousWeekIsNotFuture() {
            assertThat(LeaderboardWeekUtil.isFuture("2026-W42", FIXED_CLOCK)).isFalse();
        }

        @Test
        @DisplayName("Tuần nằm trong retention 8 tuần")
        void withinRetention() {
            // Tuần hiện tại = 2026-W43
            assertThat(LeaderboardWeekUtil.isWithinRetention("2026-W43", FIXED_CLOCK, 8)).isTrue();
            assertThat(LeaderboardWeekUtil.isWithinRetention("2026-W36", FIXED_CLOCK, 8)).isTrue();
            // 8 tuần trước = 2026-W35 → ngoài retention
            assertThat(LeaderboardWeekUtil.isWithinRetention("2026-W35", FIXED_CLOCK, 8)).isFalse();
            // Tuần tương lai → ngoài retention
            assertThat(LeaderboardWeekUtil.isWithinRetention("2026-W44", FIXED_CLOCK, 8)).isFalse();
        }
    }
}
