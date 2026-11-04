package com.jp.elearningjp.service.srs;

import com.jp.elearningjp.config.TimeConfig;
import com.jp.elearningjp.shared.enums.SrsRating;
import com.jp.elearningjp.shared.enums.SrsStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Thuật toán SM-2 (biến thể Anki) tính lịch ôn tập.
 *
 * <p><b>Quy tắc chính:</b></p>
 * <ul>
 *   <li>AGAIN: interval = 0 (hiện lại trong phiên), repetition về 0, EF −0.20.
 *       Thẻ đã "tốt nghiệp" (REVIEWING/MASTERED/RELEARNING) → RELEARNING và lapse +1.</li>
 *   <li>HARD: thẻ đang ở bước học (interval = 0) → giữ lại trong phiên, không tăng repetition.
 *       Ngược lại interval = max(interval × 1.2, interval + 1). EF −0.14.</li>
 *   <li>GOOD: 1 → 6 → interval × EF. EF giữ nguyên.</li>
 *   <li>EASY: 3 → 8 → interval × EF × 1.3. EF +0.10.</li>
 *   <li>Interval luôn bị chặn trên {@link #MAX_INTERVAL_DAYS}; EF trong [{@link #MIN_EF}, {@link #MAX_EF}].</li>
 *   <li>Trạng thái suy ra từ interval: ≥ {@link #MASTERED_THRESHOLD_DAYS} → MASTERED,
 *       ≥ {@link #REVIEWING_THRESHOLD_DAYS} → REVIEWING, còn lại LEARNING/RELEARNING.</li>
 * </ul>
 */
public final class Sm2Algorithm {

    public static final BigDecimal DEFAULT_EF = new BigDecimal("2.50");
    public static final BigDecimal MIN_EF = new BigDecimal("1.30");
    public static final BigDecimal MAX_EF = new BigDecimal("3.50");

    /** Interval tối thiểu để thẻ được coi là "đang ôn" (REVIEWING). */
    public static final int REVIEWING_THRESHOLD_DAYS = 6;
    /** Interval tối thiểu để thẻ được coi là "thành thạo" (MASTERED) — áp dụng chung mọi nút. */
    public static final int MASTERED_THRESHOLD_DAYS = 21;
    /** Trần interval — tránh hẹn ôn sau nhiều năm và tránh tràn số int. */
    public static final int MAX_INTERVAL_DAYS = 365;

    private static final BigDecimal EF_AGAIN_PENALTY = new BigDecimal("0.20");
    private static final double HARD_MULTIPLIER = 1.2;
    private static final double EASY_BONUS = 1.3;

    private Sm2Algorithm() {}

    @Getter
    @Builder
    public static class CalculationResult {
        private final BigDecimal newEf;
        private final Integer newInterval;
        private final Integer newRepetitionCount;
        private final Integer newLapseCount;
        private final SrsStatus newStatus;
        private final Instant nextReviewAt;
    }

    /** Dùng đồng hồ hệ thống, trạng thái hiện tại suy từ interval. */
    public static CalculationResult calculate(
            BigDecimal currentEf,
            Integer currentInterval,
            Integer repetitionCount,
            Integer lapseCount,
            SrsRating rating
    ) {
        ZoneId zone = ZoneId.of(TimeConfig.APP_ZONE);
        return calculate(currentEf, currentInterval, repetitionCount, lapseCount, null, rating, Clock.system(zone), zone);
    }

    /** Giữ tương thích chữ ký cũ (không truyền trạng thái hiện tại). */
    public static CalculationResult calculate(
            BigDecimal currentEf,
            Integer currentInterval,
            Integer repetitionCount,
            Integer lapseCount,
            SrsRating rating,
            Clock clock,
            ZoneId zone
    ) {
        return calculate(currentEf, currentInterval, repetitionCount, lapseCount, null, rating, clock, zone);
    }

    /**
     * Tính chu kỳ ôn tiếp theo.
     *
     * @param currentStatus trạng thái hiện tại của thẻ; {@code null} → suy ra từ interval
     */
    public static CalculationResult calculate(
            BigDecimal currentEf,
            Integer currentInterval,
            Integer repetitionCount,
            Integer lapseCount,
            SrsStatus currentStatus,
            SrsRating rating,
            Clock clock,
            ZoneId zone
    ) {
        BigDecimal ef = currentEf != null ? currentEf : DEFAULT_EF;
        int interval = currentInterval != null ? Math.max(0, currentInterval) : 0;
        int repetition = repetitionCount != null ? repetitionCount : 0;
        int lapse = lapseCount != null ? lapseCount : 0;

        // Thẻ đã từng "tốt nghiệp" khỏi giai đoạn học lần đầu?
        boolean graduated = currentStatus != null
                ? currentStatus == SrsStatus.REVIEWING
                    || currentStatus == SrsStatus.MASTERED
                    || currentStatus == SrsStatus.RELEARNING
                : interval >= REVIEWING_THRESHOLD_DAYS;
        boolean relearning = currentStatus == SrsStatus.RELEARNING;

        int newRepetition;
        int newLapse = lapse;
        long newInterval;
        BigDecimal newEf;

        switch (rating) {
            case AGAIN -> {
                newRepetition = 0;
                newInterval = 0;
                newEf = ef.subtract(EF_AGAIN_PENALTY);
                if (graduated) {
                    newLapse++;
                    relearning = true;
                }
            }
            case HARD -> {
                newEf = ef.add(BigDecimal.valueOf(sm2EfDelta(rating)));
                if (interval == 0) {
                    // Đang ở bước học: chưa đủ nhớ để giãn ngày → giữ lại trong phiên
                    newRepetition = repetition;
                    newInterval = 0;
                } else {
                    newRepetition = repetition + 1;
                    newInterval = Math.max(Math.round(interval * HARD_MULTIPLIER), interval + 1L);
                }
            }
            case GOOD -> {
                newRepetition = repetition + 1;
                newEf = ef.add(BigDecimal.valueOf(sm2EfDelta(rating)));
                if (newRepetition == 1) {
                    newInterval = 1;
                } else if (newRepetition == 2) {
                    newInterval = 6;
                } else {
                    newInterval = Math.max(Math.round(interval * ef.doubleValue()), interval + 1L);
                }
            }
            case EASY -> {
                newRepetition = repetition + 1;
                newEf = ef.add(BigDecimal.valueOf(sm2EfDelta(rating)));
                if (newRepetition == 1) {
                    newInterval = 3;
                } else if (newRepetition == 2) {
                    newInterval = 8;
                } else {
                    newInterval = Math.max(Math.round(interval * ef.doubleValue() * EASY_BONUS), interval + 1L);
                }
            }
            default -> throw new IllegalArgumentException("Unsupported rating: " + rating);
        }

        int cappedInterval = (int) Math.min(newInterval, MAX_INTERVAL_DAYS);
        newEf = clampEf(newEf);
        SrsStatus newStatus = resolveStatus(cappedInterval, relearning);

        // Thời điểm ôn tiếp theo theo ngày lịch: 00:00 (giờ VN) của ngày đến hạn
        ZoneId targetZone = zone != null ? zone : ZoneId.of(TimeConfig.APP_ZONE);
        Clock targetClock = clock != null ? clock : Clock.system(targetZone);
        Instant nextReviewAt = cappedInterval > 0
                ? LocalDate.now(targetClock).plusDays(cappedInterval).atStartOfDay(targetZone).toInstant()
                : Instant.now(targetClock);

        return CalculationResult.builder()
                .newEf(newEf)
                .newInterval(cappedInterval)
                .newRepetitionCount(newRepetition)
                .newLapseCount(newLapse)
                .newStatus(newStatus)
                .nextReviewAt(nextReviewAt)
                .build();
    }

    /** Suy ra trạng thái từ interval (một nguồn sự thật duy nhất cho mọi nút đánh giá). */
    public static SrsStatus resolveStatus(int intervalDays, boolean relearning) {
        if (intervalDays >= MASTERED_THRESHOLD_DAYS) return SrsStatus.MASTERED;
        if (intervalDays >= REVIEWING_THRESHOLD_DAYS) return SrsStatus.REVIEWING;
        return relearning ? SrsStatus.RELEARNING : SrsStatus.LEARNING;
    }

    /** EF' = EF + (0.1 − (5 − q) × (0.08 + (5 − q) × 0.02)) — công thức SM-2 chuẩn. */
    private static double sm2EfDelta(SrsRating rating) {
        double qDiff = 5.0 - rating.getQualityScore();
        return 0.1 - (qDiff * (0.08 + (qDiff * 0.02)));
    }

    private static BigDecimal clampEf(BigDecimal ef) {
        if (ef.compareTo(MIN_EF) < 0) ef = MIN_EF;
        else if (ef.compareTo(MAX_EF) > 0) ef = MAX_EF;
        return ef.setScale(2, RoundingMode.HALF_UP);
    }
}
