package com.jp.elearningjp.service.srs;

import com.jp.elearningjp.shared.enums.SrsRating;
import com.jp.elearningjp.shared.enums.SrsStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public final class Sm2Algorithm {

    public static final BigDecimal DEFAULT_EF = new BigDecimal("2.50");
    public static final BigDecimal MIN_EF = new BigDecimal("1.30");
    public static final BigDecimal MAX_EF = new BigDecimal("3.50");

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

    /**
     * Tính toán chu kỳ ôn tập tiếp theo dựa trên thuật toán SM-2 (SuperMemo-2 / Anki)
     *
     * @param currentEf Hệ số dễ dãi hiện tại
     * @param currentInterval Khoảng cách ngày ôn hiện tại
     * @param repetitionCount Số lần ôn đúng liên tiếp
     * @param lapseCount Số lần quên
     * @param rating Đánh giá chất lượng của học viên
     * @return CalculationResult chứa các chỉ số cập nhật mới
     */
    public static CalculationResult calculate(
            BigDecimal currentEf,
            Integer currentInterval,
            Integer repetitionCount,
            Integer lapseCount,
            SrsRating rating
    ) {
        if (currentEf == null) currentEf = DEFAULT_EF;
        if (currentInterval == null) currentInterval = 0;
        if (repetitionCount == null) repetitionCount = 0;
        if (lapseCount == null) lapseCount = 0;

        short q = rating.getQualityScore();
        int newRepetition;
        int newLapse = lapseCount;
        int newInterval;
        BigDecimal newEf = currentEf;
        SrsStatus newStatus;

        if (rating == SrsRating.AGAIN) {
            // Học viên quên -> Reset lại từ đầu
            newRepetition = 0;
            newLapse++;
            newInterval = 1;
            // Giảm EF: EF' = EF - 0.20
            newEf = currentEf.subtract(new BigDecimal("0.20"));
            newStatus = SrsStatus.LEARNING;
        } else {
            // Học viên nhớ được (HARD, GOOD, EASY)
            newRepetition = repetitionCount + 1;

            // Tính EF theo công thức chuẩn SM-2:
            // EF' = EF + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))
            double qDiff = 5.0 - q;
            double efDelta = 0.1 - (qDiff * (0.08 + (qDiff * 0.02)));
            newEf = currentEf.add(BigDecimal.valueOf(efDelta));

            // Điều chỉnh interval theo số lần đúng và chất lượng
            if (rating == SrsRating.HARD) {
                // HARD: Khoảng cách tăng chậm hơn bình thường (1.2 lần)
                newInterval = Math.max(1, (int) Math.round(Math.max(1, currentInterval) * 1.2));
                newStatus = currentInterval >= 6 ? SrsStatus.REVIEWING : SrsStatus.LEARNING;
            } else if (rating == SrsRating.EASY) {
                // EASY: Giãn cách tăng vọt
                if (newRepetition == 1) {
                    newInterval = 3;
                } else if (newRepetition == 2) {
                    newInterval = 8;
                } else {
                    newInterval = (int) Math.round(currentInterval * currentEf.doubleValue() * 1.3);
                }
                newStatus = newInterval >= 21 ? SrsStatus.MASTERED : SrsStatus.REVIEWING;
            } else {
                // GOOD: Theo chuẩn SM-2
                if (newRepetition == 1) {
                    newInterval = 1;
                } else if (newRepetition == 2) {
                    newInterval = 6;
                } else {
                    newInterval = (int) Math.round(currentInterval * currentEf.doubleValue());
                }
                newStatus = SrsStatus.REVIEWING;
            }

            // Kiểm tra điều kiện thông thạo (MASTERED)
            if (newInterval >= 30 && newRepetition >= 4) {
                newStatus = SrsStatus.MASTERED;
            }
        }

        // Đảm bảo EF nằm trong dải [MIN_EF, MAX_EF]
        if (newEf.compareTo(MIN_EF) < 0) {
            newEf = MIN_EF;
        } else if (newEf.compareTo(MAX_EF) > 0) {
            newEf = MAX_EF;
        }
        newEf = newEf.setScale(2, RoundingMode.HALF_UP);

        // Tính thời điểm tiếp theo (thêm số ngày mới vào Instant hiện tại)
        Instant nextReviewAt = Instant.now().plus(newInterval, ChronoUnit.DAYS);

        return CalculationResult.builder()
                .newEf(newEf)
                .newInterval(newInterval)
                .newRepetitionCount(newRepetition)
                .newLapseCount(newLapse)
                .newStatus(newStatus)
                .nextReviewAt(nextReviewAt)
                .build();
    }
}
