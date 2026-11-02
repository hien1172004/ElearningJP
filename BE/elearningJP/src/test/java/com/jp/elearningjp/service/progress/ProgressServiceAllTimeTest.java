package com.jp.elearningjp.service.progress;

import com.jp.elearningjp.dto.response.progress.ChartResponse;
import com.jp.elearningjp.dto.response.progress.ProgressSummaryResponse;
import com.jp.elearningjp.repository.content.GrammarPointRepository;
import com.jp.elearningjp.repository.content.VocabularyRepository;
import com.jp.elearningjp.repository.content.WritingCharacterRepository;
import com.jp.elearningjp.repository.gamification.StudyEventRepository;
import com.jp.elearningjp.repository.gamification.UserDailyStatsRepository;
import com.jp.elearningjp.repository.user.UserRepository;
import com.jp.elearningjp.service.config.AppConfigService;
import com.jp.elearningjp.service.gamification.StreakService;
import com.jp.elearningjp.shared.util.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProgressServiceAllTimeTest {

    private static final Long USER_ID = 7L;
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 4);
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-11-04T03:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));

    @Mock UserDailyStatsRepository dailyStatsRepository;
    @Mock StreakService streakService;
    @Mock LeaderboardService leaderboardService;
    @Mock UserRepository userRepository;
    @Mock AppConfigService appConfigService;
    @Mock CurrentUserService currentUserService;
    @Mock StudyEventRepository studyEventRepository;
    @Mock VocabularyRepository vocabularyRepository;
    @Mock GrammarPointRepository grammarPointRepository;
    @Mock WritingCharacterRepository writingCharacterRepository;
    @Mock ProgressGoalService progressGoalService;

    ProgressService service;

    @BeforeEach
    void setUp() {
        service = new ProgressService(dailyStatsRepository, streakService, leaderboardService,
                userRepository, appConfigService, currentUserService, studyEventRepository,
                vocabularyRepository, grammarPointRepository, writingCharacterRepository, CLOCK,
                progressGoalService);
        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);
    }

    private void aggregateReturnsActivity() {
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{10L, 20L, 600L, 5L, 3L, 1L, 0L, 0L, 0L});
        when(dailyStatsRepository.aggregateTwoPeriods(any(), any(), any(), any())).thenReturn(rows);
    }

    @Test
    void summary_startsFromFirstActivity_beyond366Days_withoutDelta() {
        LocalDate first = TODAY.minusYears(3);
        when(dailyStatsRepository.findFirstStatDate(USER_ID)).thenReturn(first);
        aggregateReturnsActivity();

        ProgressSummaryResponse res = service.getAllTimeSummary();

        assertThat(res.getFrom()).isEqualTo(first);
        assertThat(res.getTo()).isEqualTo(TODAY);
        assertThat(res.getWordsLearned()).isEqualTo(10L);
        assertThat(res.getWordsLearnedDeltaPct()).isNull();
        assertThat(res.getStudySecondsDeltaPct()).isNull();
    }

    @Test
    void summary_noActivityYet_isTodayOnly() {
        when(dailyStatsRepository.findFirstStatDate(USER_ID)).thenReturn(null);

        ProgressSummaryResponse res = service.getAllTimeSummary();

        assertThat(res.getFrom()).isEqualTo(TODAY);
        assertThat(res.getTo()).isEqualTo(TODAY);
    }

    @Test
    void chart_coversWholeHistory_beyond366Days() {
        LocalDate first = TODAY.minusYears(2);
        when(dailyStatsRepository.findFirstStatDate(USER_ID)).thenReturn(first);

        ChartResponse res = service.getAllTimeChart("month");

        verify(dailyStatsRepository).findCalendarRange(eq(USER_ID), eq(first), eq(TODAY));
        assertThat(res.getFrom()).isEqualTo(first);
        assertThat(res.getPoints()).hasSize(25);
    }

    @Test
    void normalSummary_stillComparesWithPreviousPeriod() {
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{10L, 20L, 600L, 5L, 3L, 1L, 5L, 0L, 300L});
        when(dailyStatsRepository.aggregateTwoPeriods(any(), any(), any(), any())).thenReturn(rows);

        ProgressSummaryResponse res = service.getSummary(USER_ID, TODAY, TODAY);

        assertThat(res.getWordsLearnedDeltaPct()).isEqualTo(100.0);
    }
}
