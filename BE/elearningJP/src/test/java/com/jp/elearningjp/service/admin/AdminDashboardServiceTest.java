package com.jp.elearningjp.service.admin;

import com.jp.elearningjp.dto.response.admin.AdminDashboardOverviewResponse;
import com.jp.elearningjp.dto.response.admin.AdminDashboardOverviewResponse.LevelStats;
import com.jp.elearningjp.dto.response.admin.AdminDashboardOverviewResponse.StreakBucket;
import com.jp.elearningjp.repository.exam.ExamAttemptRepository;
import com.jp.elearningjp.repository.gamification.UserDailyStatsRepository;
import com.jp.elearningjp.repository.gamification.UserGamificationRepository;
import com.jp.elearningjp.repository.user.UserRepository;
import com.jp.elearningjp.shared.enums.JlptLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminDashboardServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    /** 2026-11-04 00:30 giờ VN = 2026-11-03 17:30 UTC → "hôm nay" phải là 04/11, không phải 03/11. */
    private static final Instant NOW = Instant.parse("2026-11-03T17:30:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 4);

    private UserRepository userRepository;
    private UserDailyStatsRepository dailyStatsRepository;
    private UserGamificationRepository gamificationRepository;
    private ExamAttemptRepository attemptRepository;
    private AdminDashboardService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        dailyStatsRepository = mock(UserDailyStatsRepository.class);
        gamificationRepository = mock(UserGamificationRepository.class);
        attemptRepository = mock(ExamAttemptRepository.class);
        service = new AdminDashboardService(userRepository, dailyStatsRepository, gamificationRepository,
                attemptRepository, Clock.fixed(NOW, ZONE));

        // Mặc định: hệ thống rỗng
        when(userRepository.aggregateUserOverview(any(), any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{0L, 0L, 0L, 0L, 0L, 0L}));
        when(dailyStatsRepository.aggregateActivityPeriods(any(), any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{0L, 0L, 0L, 0L}));
        when(attemptRepository.aggregateAttemptOverview(any(), any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{0L, 0L, 0L}));
    }

    @Test
    void rangeBoundaries_useBusinessTimezone() {
        AdminDashboardOverviewResponse res = service.getOverview(7);

        assertThat(res.getTo()).isEqualTo(TODAY);
        assertThat(res.getFrom()).isEqualTo(TODAY.minusDays(6));
        // from = 00:00 giờ VN ngày 29/10 = 17:00 UTC ngày 28/10; kỳ trước bắt đầu sớm hơn 7 ngày
        Instant from = Instant.parse("2026-10-28T17:00:00Z");
        Instant prevFrom = Instant.parse("2026-10-21T17:00:00Z");
        Instant to = Instant.parse("2026-11-04T17:00:00Z");
        verify(userRepository).aggregateUserOverview(prevFrom, from, to);
        verify(userRepository).countNewUsersByDay(from, to, "Asia/Ho_Chi_Minh");
        verify(dailyStatsRepository).aggregateActivityPeriods(TODAY.minusDays(13), TODAY.minusDays(6), TODAY);
    }

    @Test
    void days_isClampedTo1And90() {
        assertThat(service.getOverview(0).getDays()).isEqualTo(1);
        assertThat(service.getOverview(500).getDays()).isEqualTo(90);
        assertThat(service.getOverview(500).getDaily()).hasSize(90);
    }

    @Test
    void daily_fillsMissingDaysWithZero_andMergesSources() {
        when(userRepository.countNewUsersByDay(any(), any(), anyString()))
                .thenReturn(List.<Object[]>of(new Object[]{java.sql.Date.valueOf(TODAY.minusDays(1)), 3L}));
        when(dailyStatsRepository.aggregateActivityByDay(any(), any()))
                .thenReturn(List.<Object[]>of(
                        new Object[]{TODAY, 5L, 1200L, 2L},
                        new Object[]{TODAY.minusDays(2), 1L, 60L, 1L}));

        AdminDashboardOverviewResponse res = service.getOverview(7);

        assertThat(res.getDaily()).hasSize(7);
        assertThat(res.getDaily().get(0).getDate()).isEqualTo(TODAY.minusDays(6));
        assertThat(res.getDaily().get(6).getActiveUsers()).isEqualTo(5);
        assertThat(res.getDaily().get(5).getNewUsers()).isEqualTo(3);
        assertThat(res.getDaily().get(3).getActiveUsers()).isZero();
        assertThat(res.getActivity().getActiveToday()).isEqualTo(5);
        assertThat(res.getActivity().getGoalMetCount()).isEqualTo(3);
        assertThat(res.getActivity().getAvgDailyActive()).isEqualTo(0.9); // (5+1)/7
    }

    @Test
    void streakBuckets_bucketZeroIsRemainderOfTotalUsers() {
        when(userRepository.aggregateUserOverview(any(), any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{10L, 2L, 1L, 1L, 0L, 4L}));
        when(gamificationRepository.countActiveStreakBuckets(TODAY.minusDays(1)))
                .thenReturn(List.<Object[]>of(new Object[]{1, 3L}, new Object[]{4, 1L}));

        AdminDashboardOverviewResponse res = service.getOverview(30);

        assertThat(res.getActivity().getStreakBuckets())
                .extracting(StreakBucket::getLabel, StreakBucket::getUsers)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("0", 6L),
                        org.assertj.core.groups.Tuple.tuple("1–2", 3L),
                        org.assertj.core.groups.Tuple.tuple("3–6", 0L),
                        org.assertj.core.groups.Tuple.tuple("7–29", 0L),
                        org.assertj.core.groups.Tuple.tuple("30+", 1L));
        assertThat(res.getUsers().getLocalAccounts()).isEqualTo(6);
    }

    @Test
    void examStats_alwaysFiveLevels_weightedAverageAndPassRate() {
        when(attemptRepository.aggregateAttemptsByLevel(any(), any()))
                .thenReturn(List.<Object[]>of(
                        // level, submitted, graded, passed, avg, scored
                        new Object[]{"N5", 4L, 4L, 3L, new BigDecimal("120.0"), 4L},
                        new Object[]{"N3", 2L, 1L, 0L, new BigDecimal("60.0"), 1L}));

        AdminDashboardOverviewResponse res = service.getOverview(30);

        List<LevelStats> levels = res.getExams().getByLevel();
        assertThat(levels).extracting(LevelStats::getLevel)
                .containsExactly(JlptLevel.N5, JlptLevel.N4, JlptLevel.N3, JlptLevel.N2, JlptLevel.N1);
        assertThat(levels.get(0).getPassRate()).isEqualTo(0.75);
        assertThat(levels.get(1).getPassRate()).isNull(); // N4 không có bài
        assertThat(res.getExams().getPassRate()).isEqualTo(0.6); // 3 / 5 bài có kết quả
        assertThat(res.getExams().getAvgScaledScore()).isEqualTo(108.0); // (120*4 + 60*1) / 5
    }

    @Test
    void emptySystem_returnsZerosAndNulls() {
        AdminDashboardOverviewResponse res = service.getOverview(30);

        assertThat(res.getUsers().getTotal()).isZero();
        assertThat(res.getExams().getPassRate()).isNull();
        assertThat(res.getExams().getAvgScaledScore()).isNull();
        assertThat(res.getActivity().getStreakBuckets()).hasSize(5);
        verify(gamificationRepository).countActiveStreakBuckets(eq(TODAY.minusDays(1)));
    }
}
