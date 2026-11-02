package com.jp.elearningjp.service.progress;

import com.jp.elearningjp.dto.response.progress.LeaderboardResponse;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
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
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProgressServiceLeaderboardTest {

    private static final String WEEK = "2026-W45";
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
        when(appConfigService.getIntValue(any(), anyInt())).thenReturn(1);
        when(currentUserService.getCurrentUserId()).thenReturn(7L);
    }

    private static List<Object[]> scores(long... userAndScore) {
        List<Object[]> rows = new java.util.ArrayList<>();
        for (int i = 0; i < userAndScore.length; i += 2) {
            rows.add(new Object[]{userAndScore[i], userAndScore[i + 1]});
        }
        return rows;
    }

    @Test
    void getLeaderboard_redisMiss_losingLock_readsLimitedTopOnly() {
        when(leaderboardService.getTop(anyInt(), eq(WEEK))).thenReturn(List.of());
        when(leaderboardService.tryAcquireRebuildLock(WEEK)).thenReturn(false);
        when(dailyStatsRepository.findTopUsersByWeek(any(), any(), eq(5), anyInt(), anyInt()))
                .thenReturn(scores(1L, 10L));
        when(dailyStatsRepository.countUsersWithScoreInWeek(any(), any(), anyInt(), anyInt())).thenReturn(42L);

        LeaderboardResponse res = service.getLeaderboard(WEEK);

        verify(dailyStatsRepository, never()).findAllScoresByWeek(any(), any(), anyInt(), anyInt());
        verify(leaderboardService, never()).rebuild(any(), anyList());
        verify(leaderboardService, never()).releaseRebuildLock(any());
        assertThat(res.getEntries()).hasSize(1);
        assertThat(res.getTotalPlayers()).isEqualTo(42L);
    }

    @Test
    void getLeaderboard_redisMiss_winningLock_rebuildsAndReleasesLock() {
        when(leaderboardService.getTop(anyInt(), eq(WEEK))).thenReturn(List.of());
        when(leaderboardService.tryAcquireRebuildLock(WEEK)).thenReturn(true);
        when(dailyStatsRepository.findAllScoresByWeek(any(), any(), anyInt(), anyInt()))
                .thenReturn(scores(1L, 10L, 2L, 9L, 3L, 8L, 4L, 7L, 5L, 6L, 6L, 5L));

        LeaderboardResponse res = service.getLeaderboard(WEEK);

        verify(leaderboardService).rebuild(eq(WEEK), anyList());
        verify(leaderboardService).releaseRebuildLock(WEEK);
        assertThat(res.getEntries()).hasSize(5);
        assertThat(res.getTotalPlayers()).isEqualTo(6L);
    }

    @Test
    void refresh_usesPerUserCooldown_andNotHardCooldown() {
        when(leaderboardService.acquireRefreshCooldown(WEEK, 7L)).thenReturn(false);

        assertThatThrownBy(() -> service.refreshLeaderboard(WEEK, false))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.LEADERBOARD_REFRESH_TOO_FREQUENT);

        verify(leaderboardService, never()).acquireHardRefreshCooldown(any());
    }

    @Test
    void hardRefresh_usesOwnCooldown_notBlockedByUserCooldown() {
        when(leaderboardService.acquireRefreshCooldown(any(), any())).thenReturn(false);
        when(leaderboardService.acquireHardRefreshCooldown(WEEK)).thenReturn(true);
        when(leaderboardService.tryAcquireRebuildLock(WEEK)).thenReturn(true);
        when(leaderboardService.hardRebuild(eq(WEEK), anyList())).thenReturn(true);
        when(dailyStatsRepository.findAllScoresByWeek(any(), any(), anyInt(), anyInt()))
                .thenReturn(Collections.emptyList());

        service.refreshLeaderboard(WEEK, true);

        verify(leaderboardService).hardRebuild(eq(WEEK), anyList());
        verify(leaderboardService).releaseRebuildLock(WEEK);
    }

    @Test
    void hardRefresh_lockBusy_throwsInsteadOfReportingSuccess() {
        when(leaderboardService.acquireHardRefreshCooldown(WEEK)).thenReturn(true);
        when(leaderboardService.tryAcquireRebuildLock(WEEK)).thenReturn(false);

        assertThatThrownBy(() -> service.refreshLeaderboard(WEEK, true))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.LEADERBOARD_REBUILD_IN_PROGRESS);

        verify(leaderboardService, never()).hardRebuild(any(), anyList());
        verify(leaderboardService, never()).releaseRebuildLock(any());
    }

    @Test
    void hardRefresh_redisFailure_throwsAndReleasesLock() {
        when(leaderboardService.acquireHardRefreshCooldown(WEEK)).thenReturn(true);
        when(leaderboardService.tryAcquireRebuildLock(WEEK)).thenReturn(true);
        when(leaderboardService.hardRebuild(eq(WEEK), anyList())).thenReturn(false);
        when(dailyStatsRepository.findAllScoresByWeek(any(), any(), anyInt(), anyInt()))
                .thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> service.refreshLeaderboard(WEEK, true))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.LEADERBOARD_REBUILD_FAILED);

        verify(leaderboardService).releaseRebuildLock(WEEK);
    }

    @Test
    void refresh_lockBusy_servesLimitedTopWithoutError() {
        when(leaderboardService.acquireRefreshCooldown(WEEK, 7L)).thenReturn(true);
        when(leaderboardService.tryAcquireRebuildLock(WEEK)).thenReturn(false);
        when(dailyStatsRepository.findTopUsersByWeek(any(), any(), eq(5), anyInt(), anyInt()))
                .thenReturn(scores(1L, 10L));

        assertThat(service.refreshLeaderboard(WEEK, false).getEntries()).hasSize(1);

        verify(dailyStatsRepository, never()).findAllScoresByWeek(any(), any(), anyInt(), anyInt());
    }
}
