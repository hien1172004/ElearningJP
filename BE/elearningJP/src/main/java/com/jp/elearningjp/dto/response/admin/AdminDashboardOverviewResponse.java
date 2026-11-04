package com.jp.elearningjp.dto.response.admin;

import com.jp.elearningjp.shared.enums.JlptLevel;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;

/**
 * Số liệu tổng quan cho dashboard quản trị.
 * Kỳ hiện tại là {@code days} ngày gần nhất tính tới hết hôm nay (múi giờ Asia/Ho_Chi_Minh);
 * các trường {@code *Previous} là kỳ liền trước có cùng độ dài, dùng để so sánh xu hướng.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminDashboardOverviewResponse {

    int days;
    LocalDate from;
    LocalDate to;

    UserStats users;
    ActivityStats activity;
    ExamStats exams;
    /** Một phần tử cho mỗi ngày trong kỳ (kể cả ngày không có số liệu = 0), tăng dần theo ngày. */
    List<DailyPoint> daily;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class UserStats {
        long total;
        long newInRange;
        long newPrevious;
        long banned;
        long inactive;
        long googleAccounts;
        long localAccounts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ActivityStats {
        /** Số user có hoạt động học hôm nay. */
        long activeToday;
        /** Số user (không trùng) có hoạt động trong kỳ / kỳ trước. */
        long activeInRange;
        long activePrevious;
        /** Trung bình số user hoạt động mỗi ngày trong kỳ. */
        double avgDailyActive;
        long studySecondsInRange;
        long studySecondsPrevious;
        /** Tổng lượt "user-ngày" đạt mục tiêu ngày trong kỳ. */
        long goalMetCount;
        /** Phân bố streak hiện tại (ước tính từ dữ liệu cache), thứ tự từ 0 ngày → 30+ ngày. */
        List<StreakBucket> streakBuckets;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class StreakBucket {
        /** Nhãn hiển thị, vd "0", "1–2", "30+". */
        String label;
        long users;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ExamStats {
        long submittedInRange;
        long submittedPrevious;
        /** Bài bắt đầu trong kỳ nhưng chưa nộp (bỏ dở). */
        long inProgress;
        /** Tỉ lệ đỗ (0–1) trên các bài có kết quả đỗ/trượt; null nếu chưa có bài nào. */
        Double passRate;
        /** Điểm quy đổi trung bình; null nếu chưa có bài nào có điểm. */
        Double avgScaledScore;
        /** Luôn đủ 5 cấp N5 → N1 (cấp không có bài thì số liệu = 0/null). */
        List<LevelStats> byLevel;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class LevelStats {
        JlptLevel level;
        long submitted;
        long graded;
        long passed;
        Double passRate;
        Double avgScaledScore;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class DailyPoint {
        LocalDate date;
        long activeUsers;
        long newUsers;
        long studySeconds;
    }
}
