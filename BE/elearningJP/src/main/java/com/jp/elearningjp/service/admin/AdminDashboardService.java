package com.jp.elearningjp.service.admin;

import com.jp.elearningjp.dto.response.admin.AdminDashboardOverviewResponse;
import com.jp.elearningjp.dto.response.admin.AdminDashboardOverviewResponse.*;
import com.jp.elearningjp.repository.exam.ExamAttemptRepository;
import com.jp.elearningjp.repository.gamification.UserDailyStatsRepository;
import com.jp.elearningjp.repository.gamification.UserGamificationRepository;
import com.jp.elearningjp.repository.user.UserRepository;
import com.jp.elearningjp.shared.enums.JlptLevel;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * Tổng hợp số liệu cho dashboard quản trị: người dùng, mức độ hoạt động, thi thử JLPT.
 *
 * <p>Toàn bộ phép đếm/gộp chạy trong DB (vài query aggregate), không tải bản ghi lên RAM.
 * Mốc "ngày" lấy theo {@link Clock} nghiệp vụ (Asia/Ho_Chi_Minh).</p>
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminDashboardService {

    static final int MIN_DAYS = 1;
    static final int MAX_DAYS = 90;

    /** Nhãn nhóm streak theo thứ tự bucket 0..4 (bucket 1..4 khớp query countActiveStreakBuckets). */
    static final List<String> STREAK_LABELS = List.of("0", "1–2", "3–6", "7–29", "30+");

    UserRepository userRepository;
    UserDailyStatsRepository userDailyStatsRepository;
    UserGamificationRepository userGamificationRepository;
    ExamAttemptRepository examAttemptRepository;
    Clock clock;

    @Transactional(readOnly = true)
    public AdminDashboardOverviewResponse getOverview(int requestedDays) {
        int days = Math.min(Math.max(requestedDays, MIN_DAYS), MAX_DAYS);
        ZoneId zone = clock.getZone();

        // Kỳ hiện tại: [from, today] (gồm hôm nay); kỳ trước: [prevFrom, from)
        LocalDate today = LocalDate.now(clock);
        LocalDate from = today.minusDays(days - 1L);
        LocalDate prevFrom = from.minusDays(days);

        Instant fromInstant = from.atStartOfDay(zone).toInstant();
        Instant prevFromInstant = prevFrom.atStartOfDay(zone).toInstant();
        Instant toInstant = today.plusDays(1).atStartOfDay(zone).toInstant();

        UserStats users = buildUserStats(prevFromInstant, fromInstant, toInstant);
        Map<LocalDate, DailyPoint> daily = initDaily(from, today);

        // ----- Người dùng mới theo ngày
        for (Object[] row : userRepository.countNewUsersByDay(fromInstant, toInstant, zone.getId())) {
            DailyPoint p = daily.get(toLocalDate(row[0]));
            if (p != null) p.setNewUsers(toLong(row[1]));
        }

        // ----- Hoạt động theo ngày
        long goalMet = 0;
        for (Object[] row : userDailyStatsRepository.aggregateActivityByDay(from, today)) {
            DailyPoint p = daily.get(toLocalDate(row[0]));
            if (p == null) continue;
            p.setActiveUsers(toLong(row[1]));
            p.setStudySeconds(toLong(row[2]));
            goalMet += toLong(row[3]);
        }

        Object[] periods = firstRow(userDailyStatsRepository.aggregateActivityPeriods(prevFrom, from, today), 4);
        long activeTotalPerDay = daily.values().stream().mapToLong(DailyPoint::getActiveUsers).sum();

        ActivityStats activity = ActivityStats.builder()
                .activeToday(daily.get(today).getActiveUsers())
                .activeInRange(toLong(periods[0]))
                .activePrevious(toLong(periods[1]))
                .avgDailyActive(round1((double) activeTotalPerDay / days))
                .studySecondsInRange(toLong(periods[2]))
                .studySecondsPrevious(toLong(periods[3]))
                .goalMetCount(goalMet)
                .streakBuckets(buildStreakBuckets(users.getTotal(), today.minusDays(1)))
                .build();

        return AdminDashboardOverviewResponse.builder()
                .days(days)
                .from(from)
                .to(today)
                .users(users)
                .activity(activity)
                .exams(buildExamStats(prevFromInstant, fromInstant, toInstant))
                .daily(new ArrayList<>(daily.values()))
                .build();
    }

    // =========================================================
    // PRIVATE HELPERS
    // =========================================================

    private UserStats buildUserStats(Instant prevFrom, Instant from, Instant to) {
        Object[] r = firstRow(userRepository.aggregateUserOverview(prevFrom, from, to), 6);
        long total = toLong(r[0]);
        long google = toLong(r[5]);
        return UserStats.builder()
                .total(total)
                .newInRange(toLong(r[1]))
                .newPrevious(toLong(r[2]))
                .banned(toLong(r[3]))
                .inactive(toLong(r[4]))
                .googleAccounts(google)
                .localAccounts(total - google)
                .build();
    }

    private List<StreakBucket> buildStreakBuckets(long totalUsers, LocalDate yesterday) {
        long[] counts = new long[STREAK_LABELS.size()];
        for (Object[] row : userGamificationRepository.countActiveStreakBuckets(yesterday)) {
            int bucket = (int) toLong(row[0]);
            if (bucket >= 1 && bucket < counts.length) counts[bucket] = toLong(row[1]);
        }
        // Nhóm 0 = mọi user còn lại (streak đã đứt hoặc chưa từng học)
        long withStreak = Arrays.stream(counts).sum();
        counts[0] = Math.max(0, totalUsers - withStreak);

        List<StreakBucket> result = new ArrayList<>();
        for (int i = 0; i < counts.length; i++) {
            result.add(StreakBucket.builder().label(STREAK_LABELS.get(i)).users(counts[i]).build());
        }
        return result;
    }

    private ExamStats buildExamStats(Instant prevFrom, Instant from, Instant to) {
        Object[] overview = firstRow(examAttemptRepository.aggregateAttemptOverview(prevFrom, from, to), 3);

        Map<JlptLevel, LevelStats> byLevel = new EnumMap<>(JlptLevel.class);
        for (JlptLevel level : JlptLevel.values()) {
            byLevel.put(level, LevelStats.builder().level(level).build());
        }

        long gradedTotal = 0;
        long passedTotal = 0;
        double scoreSum = 0;
        long scoredTotal = 0;
        for (Object[] row : examAttemptRepository.aggregateAttemptsByLevel(from, to)) {
            JlptLevel level = parseLevel(row[0]);
            if (level == null) continue;
            long graded = toLong(row[2]);
            long passed = toLong(row[3]);
            Double avg = toDouble(row[4]);
            long scored = toLong(row[5]);

            LevelStats s = byLevel.get(level);
            s.setSubmitted(toLong(row[1]));
            s.setGraded(graded);
            s.setPassed(passed);
            s.setPassRate(ratio(passed, graded));
            s.setAvgScaledScore(avg == null ? null : round1(avg));

            gradedTotal += graded;
            passedTotal += passed;
            if (avg != null) {
                scoreSum += avg * scored;
                scoredTotal += scored;
            }
        }

        return ExamStats.builder()
                .submittedInRange(toLong(overview[0]))
                .submittedPrevious(toLong(overview[1]))
                .inProgress(toLong(overview[2]))
                .passRate(ratio(passedTotal, gradedTotal))
                .avgScaledScore(scoredTotal == 0 ? null : round1(scoreSum / scoredTotal))
                .byLevel(new ArrayList<>(byLevel.values()))
                .build();
    }

    private static Map<LocalDate, DailyPoint> initDaily(LocalDate from, LocalDate to) {
        Map<LocalDate, DailyPoint> map = new LinkedHashMap<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            map.put(d, DailyPoint.builder().date(d).build());
        }
        return map;
    }

    /** Query aggregate không GROUP BY luôn trả đúng 1 dòng; phòng hờ trả mảng 0 nếu rỗng. */
    private static Object[] firstRow(List<Object[]> rows, int width) {
        return rows == null || rows.isEmpty() || rows.get(0) == null ? new Object[width] : rows.get(0);
    }

    private static long toLong(Object o) {
        return o instanceof Number n ? n.longValue() : 0L;
    }

    private static Double toDouble(Object o) {
        return o instanceof Number n ? n.doubleValue() : null;
    }

    private static LocalDate toLocalDate(Object o) {
        if (o instanceof LocalDate d) return d;
        if (o instanceof java.sql.Date d) return d.toLocalDate();
        return o == null ? null : LocalDate.parse(o.toString());
    }

    private static JlptLevel parseLevel(Object o) {
        if (o == null) return null;
        try {
            return JlptLevel.valueOf(o.toString().trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Double ratio(long part, long whole) {
        return whole == 0 ? null : Math.round(part * 1000.0 / whole) / 1000.0;
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
