package com.jp.elearningjp.service.srs;

import com.jp.elearningjp.shared.enums.SrsRating;
import com.jp.elearningjp.shared.enums.SrsStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class Sm2AlgorithmTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Instant NOW = LocalDate.of(2026, 11, 5).atTime(10, 0).atZone(ZONE).toInstant();
    private static final Clock CLOCK = Clock.fixed(NOW, ZONE);
    private static final BigDecimal EF = new BigDecimal("2.50");

    private Sm2Algorithm.CalculationResult calc(int interval, int rep, int lapse, SrsStatus status, SrsRating rating) {
        return calc(EF, interval, rep, lapse, status, rating);
    }

    private Sm2Algorithm.CalculationResult calc(BigDecimal ef, int interval, int rep, int lapse, SrsStatus status, SrsRating rating) {
        return Sm2Algorithm.calculate(ef, interval, rep, lapse, status, rating, CLOCK, ZONE);
    }

    @Test
    @DisplayName("NEW + GOOD → LEARNING, 1 ngày, hẹn 00:00 ngày mai")
    void newCardGood() {
        var r = calc(0, 0, 0, SrsStatus.NEW, SrsRating.GOOD);
        assertThat(r.getNewInterval()).isEqualTo(1);
        assertThat(r.getNewStatus()).isEqualTo(SrsStatus.LEARNING);
        assertThat(r.getNextReviewAt()).isEqualTo(LocalDate.of(2026, 11, 6).atStartOfDay(ZONE).toInstant());
    }

    @Test
    @DisplayName("Chuỗi GOOD: 1 → 6 (REVIEWING) → 15 → 38 (MASTERED)")
    void goodSequence() {
        var r1 = calc(0, 0, 0, SrsStatus.NEW, SrsRating.GOOD);
        var r2 = calc(r1.getNewEf(), r1.getNewInterval(), r1.getNewRepetitionCount(), 0, r1.getNewStatus(), SrsRating.GOOD);
        assertThat(r2.getNewInterval()).isEqualTo(6);
        assertThat(r2.getNewStatus()).isEqualTo(SrsStatus.REVIEWING);
        var r3 = calc(r2.getNewEf(), r2.getNewInterval(), r2.getNewRepetitionCount(), 0, r2.getNewStatus(), SrsRating.GOOD);
        assertThat(r3.getNewInterval()).isEqualTo(15);
        assertThat(r3.getNewStatus()).isEqualTo(SrsStatus.REVIEWING);
        var r4 = calc(r3.getNewEf(), r3.getNewInterval(), r3.getNewRepetitionCount(), 0, r3.getNewStatus(), SrsRating.GOOD);
        assertThat(r4.getNewInterval()).isEqualTo(38);
        assertThat(r4.getNewStatus()).isEqualTo(SrsStatus.MASTERED);
    }

    @Test
    @DisplayName("NEW + AGAIN → LEARNING, hiện lại ngay, không tính lapse")
    void newCardAgain() {
        var r = calc(0, 0, 0, SrsStatus.NEW, SrsRating.AGAIN);
        assertThat(r.getNewInterval()).isZero();
        assertThat(r.getNewStatus()).isEqualTo(SrsStatus.LEARNING);
        assertThat(r.getNewLapseCount()).isZero();
        assertThat(r.getNextReviewAt()).isEqualTo(NOW);
        assertThat(r.getNewEf()).isEqualByComparingTo("2.30");
    }

    @Test
    @DisplayName("REVIEWING + AGAIN → RELEARNING, lapse +1, rồi GOOD 1 → 6 quay về REVIEWING")
    void lapseAndRelearn() {
        var r = calc(15, 3, 0, SrsStatus.REVIEWING, SrsRating.AGAIN);
        assertThat(r.getNewStatus()).isEqualTo(SrsStatus.RELEARNING);
        assertThat(r.getNewLapseCount()).isEqualTo(1);
        assertThat(r.getNewRepetitionCount()).isZero();

        var r2 = calc(r.getNewEf(), 0, 0, 1, SrsStatus.RELEARNING, SrsRating.GOOD);
        assertThat(r2.getNewInterval()).isEqualTo(1);
        assertThat(r2.getNewStatus()).isEqualTo(SrsStatus.RELEARNING);

        var r3 = calc(r2.getNewEf(), 1, 1, 1, SrsStatus.RELEARNING, SrsRating.GOOD);
        assertThat(r3.getNewInterval()).isEqualTo(6);
        assertThat(r3.getNewStatus()).isEqualTo(SrsStatus.REVIEWING);
    }

    @Test
    @DisplayName("MASTERED + AGAIN → RELEARNING")
    void masteredAgain() {
        var r = calc(40, 5, 0, SrsStatus.MASTERED, SrsRating.AGAIN);
        assertThat(r.getNewStatus()).isEqualTo(SrsStatus.RELEARNING);
        assertThat(r.getNewLapseCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("HARD ở bước học (interval 0) → giữ trong phiên, không tăng repetition")
    void hardInLearningStep() {
        var r = calc(0, 0, 0, SrsStatus.LEARNING, SrsRating.HARD);
        assertThat(r.getNewInterval()).isZero();
        assertThat(r.getNewRepetitionCount()).isZero();
        assertThat(r.getNextReviewAt()).isEqualTo(NOW);
        assertThat(r.getNewEf()).isEqualByComparingTo("2.36");
    }

    @Test
    @DisplayName("HARD không bao giờ kẹt: interval luôn tăng ít nhất 1 ngày")
    void hardAlwaysGrows() {
        assertThat(calc(1, 1, 0, SrsStatus.LEARNING, SrsRating.HARD).getNewInterval()).isEqualTo(2);
        assertThat(calc(2, 2, 0, SrsStatus.LEARNING, SrsRating.HARD).getNewInterval()).isEqualTo(3);
        assertThat(calc(10, 3, 0, SrsStatus.REVIEWING, SrsRating.HARD).getNewInterval()).isEqualTo(12);
    }

    @Test
    @DisplayName("MASTERED + HARD tụt dưới 21 ngày → REVIEWING")
    void masteredHardDowngrade() {
        // interval 21 × 1.2 = 25 → vẫn MASTERED
        assertThat(calc(21, 4, 0, SrsStatus.MASTERED, SrsRating.HARD).getNewStatus()).isEqualTo(SrsStatus.MASTERED);
        // resolveStatus là nguồn sự thật duy nhất
        assertThat(Sm2Algorithm.resolveStatus(20, false)).isEqualTo(SrsStatus.REVIEWING);
    }

    @Test
    @DisplayName("Chuỗi EASY: 3 → 8 (REVIEWING) → 28 (MASTERED, ngưỡng chung 21)")
    void easySequence() {
        var r1 = calc(0, 0, 0, SrsStatus.NEW, SrsRating.EASY);
        assertThat(r1.getNewInterval()).isEqualTo(3);
        assertThat(r1.getNewStatus()).isEqualTo(SrsStatus.LEARNING);
        var r2 = calc(r1.getNewEf(), 3, 1, 0, r1.getNewStatus(), SrsRating.EASY);
        assertThat(r2.getNewInterval()).isEqualTo(8);
        assertThat(r2.getNewStatus()).isEqualTo(SrsStatus.REVIEWING);
        var r3 = calc(r2.getNewEf(), 8, 2, 0, r2.getNewStatus(), SrsRating.EASY);
        assertThat(r3.getNewInterval()).isEqualTo(28);
        assertThat(r3.getNewStatus()).isEqualTo(SrsStatus.MASTERED);
    }

    @Test
    @DisplayName("Interval bị chặn ở 365 ngày, không tràn số")
    void intervalCap() {
        var r = calc(new BigDecimal("3.50"), 300, 8, 0, SrsStatus.MASTERED, SrsRating.EASY);
        assertThat(r.getNewInterval()).isEqualTo(Sm2Algorithm.MAX_INTERVAL_DAYS);

        var huge = calc(new BigDecimal("3.50"), Integer.MAX_VALUE / 2, 20, 0, SrsStatus.MASTERED, SrsRating.EASY);
        assertThat(huge.getNewInterval()).isEqualTo(Sm2Algorithm.MAX_INTERVAL_DAYS);
        assertThat(huge.getNextReviewAt()).isAfter(NOW);
    }

    @Test
    @DisplayName("EF bị kẹp trong [1.3, 3.5]")
    void efClamp() {
        assertThat(calc(new BigDecimal("1.30"), 6, 2, 0, SrsStatus.REVIEWING, SrsRating.AGAIN).getNewEf())
                .isEqualByComparingTo("1.30");
        assertThat(calc(new BigDecimal("3.50"), 6, 2, 0, SrsStatus.REVIEWING, SrsRating.EASY).getNewEf())
                .isEqualByComparingTo("3.50");
    }

    @Test
    @DisplayName("Chữ ký cũ (không trạng thái) suy ra graduated từ interval")
    void legacySignature() {
        var r = Sm2Algorithm.calculate(EF, 10, 3, 0, SrsRating.AGAIN, CLOCK, ZONE);
        assertThat(r.getNewStatus()).isEqualTo(SrsStatus.RELEARNING);
        assertThat(r.getNewLapseCount()).isEqualTo(1);
    }
}
