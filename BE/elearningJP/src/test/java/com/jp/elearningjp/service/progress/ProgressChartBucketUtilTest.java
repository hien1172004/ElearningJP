package com.jp.elearningjp.service.progress;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test cho {@link ProgressChartBucketUtil} — gom bucket cho biểu đồ.
 *
 * <p>Không cần Redis/DB vì đây là thuần phép tính lịch.</p>
 */
class ProgressChartBucketUtilTest {

    // =====================================================================
    // resolveGranularity
    // =====================================================================

    @Nested
    @DisplayName("Chuẩn hoá granularity")
    class Granularity {

        @ParameterizedTest
        @CsvSource({
                "day,day",
                "week,week",
                "month,month",
                "WEEK,week",
                "Month,month",
                "'  week  ',week",
        })
        @DisplayName("Granularity hợp lệ được giữ nguyên (không phân biệt hoa/thường)")
        void keepsValid(String input, String expected) {
            assertThat(ProgressChartBucketUtil.resolveGranularity(input)).isEqualTo(expected);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"hour", "weekday", "nonsense"})
        @DisplayName("Granularity sai / rỗng → day")
        void fallsBackToDay(String input) {
            assertThat(ProgressChartBucketUtil.resolveGranularity(input)).isEqualTo("day");
        }
    }

    // =====================================================================
    // bucketStartOf
    // =====================================================================

    @Nested
    @DisplayName("Ngày đầu bucket")
    class BucketStart {

        @Test
        @DisplayName("day → chính ngày đó")
        void day() {
            assertThat(ProgressChartBucketUtil.bucketStartOf(
                    LocalDate.of(2026, 10, 15), "day"))
                    .isEqualTo(LocalDate.of(2026, 10, 15));
        }

        @ParameterizedTest
        @CsvSource({
                // 2026-10-15 là thứ 5 → tuần ISO 2026-W42 bắt đầu thứ 2 12/10
                "2026-10-15,2026-10-12",
                // Chủ nhật → vẫn thuộc tuần bắt đầu thứ 2 12/10
                "2026-10-18,2026-10-12",
                // Thứ 2 đầu tuần
                "2026-10-19,2026-10-19",
        })
        @DisplayName("week → thứ 2 ISO của tuần chứa ngày đó")
        void week(String date, String expected) {
            assertThat(ProgressChartBucketUtil.bucketStartOf(
                    LocalDate.parse(date), "week"))
                    .isEqualTo(LocalDate.parse(expected));
        }

        @Test
        @DisplayName("week khớp với LeaderboardWeekUtil (cùng nguồn sự thật)")
        void weekMatchesLeaderboardUtil() {
            LocalDate date = LocalDate.of(2026, 10, 15);
            String weekKey = LeaderboardWeekUtil.format(
                    date.get(IsoFields.WEEK_BASED_YEAR),
                    date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));

            assertThat(ProgressChartBucketUtil.bucketStartOf(date, "week"))
                    .isEqualTo(LeaderboardWeekUtil.weekStart(weekKey));
            // Ngày đầu tuần luôn là thứ 2.
            assertThat(ProgressChartBucketUtil.bucketStartOf(date, "week")
                    .getDayOfWeek().getValue()).isEqualTo(1);
        }

        @ParameterizedTest
        @CsvSource({
                "2026-10-15,2026-10-01",
                "2026-01-01,2026-01-01",
                "2026-12-31,2026-12-01",
        })
        @DisplayName("month → ngày 1 của tháng")
        void month(String date, String expected) {
            assertThat(ProgressChartBucketUtil.bucketStartOf(
                    LocalDate.parse(date), "month"))
                    .isEqualTo(LocalDate.parse(expected));
        }
    }

    // =====================================================================
    // buildBuckets
    // =====================================================================

    @Nested
    @DisplayName("Dựng danh sách bucket")
    class Buckets {

        @Test
        @DisplayName("day → 1 bucket cho mỗi ngày trong kỳ")
        void dayBuckets() {
            List<LocalDate> buckets = ProgressChartBucketUtil.buildBuckets(
                    LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5), "day");

            assertThat(buckets).hasSize(5);
            assertThat(buckets.get(0)).isEqualTo(LocalDate.of(2026, 10, 1));
            assertThat(buckets.get(4)).isEqualTo(LocalDate.of(2026, 10, 5));
        }

        @Test
        @DisplayName("week → bucket đầu là thứ 2 gần nhất (có thể trước `from`)")
        void weekBuckets() {
            // from = 2026-10-15 (thứ 5) → bucket đầu = thứ 2 12/10
            List<LocalDate> buckets = ProgressChartBucketUtil.buildBuckets(
                    LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 25), "week");

            assertThat(buckets).hasSize(2);
            assertThat(buckets.get(0)).isEqualTo(LocalDate.of(2026, 10, 12));
            assertThat(buckets.get(1)).isEqualTo(LocalDate.of(2026, 10, 19));
        }

        @Test
        @DisplayName("month → 1 bucket cho mỗi tháng trong kỳ")
        void monthBuckets() {
            List<LocalDate> buckets = ProgressChartBucketUtil.buildBuckets(
                    LocalDate.of(2026, 1, 15), LocalDate.of(2026, 3, 20), "month");

            assertThat(buckets).hasSize(3);
            assertThat(buckets.get(0)).isEqualTo(LocalDate.of(2026, 1, 1));
            assertThat(buckets.get(1)).isEqualTo(LocalDate.of(2026, 2, 1));
            assertThat(buckets.get(2)).isEqualTo(LocalDate.of(2026, 3, 1));
        }

        @Test
        @DisplayName("from > to → danh sách rỗng")
        void emptyWhenRangeInverted() {
            assertThat(ProgressChartBucketUtil.buildBuckets(
                    LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 1), "day")).isEmpty();
        }

        @Test
        @DisplayName("from == to → đúng 1 bucket")
        void singleDay() {
            assertThat(ProgressChartBucketUtil.buildBuckets(
                    LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 10), "day"))
                    .hasSize(1);
        }
    }
}
