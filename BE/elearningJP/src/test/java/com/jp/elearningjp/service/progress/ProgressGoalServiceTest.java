package com.jp.elearningjp.service.progress;

import com.jp.elearningjp.dto.response.progress.ProgressGoalResponse;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.entity.user.UserProfile;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.user.UserProfileRepository;
import com.jp.elearningjp.service.gamification.DailyStatsService;
import com.jp.elearningjp.service.user.UserService;
import com.jp.elearningjp.shared.util.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProgressGoalServiceTest {

    private static final Long USER_ID = 7L;

    @Mock UserProfileRepository userProfileRepository;
    @Mock UserService userService;
    @Mock DailyStatsService dailyStatsService;
    @Mock CurrentUserService currentUserService;

    @InjectMocks ProgressGoalService service;

    /** Toàn bộ số từ/ngày hiển thị trên màn hình chọn mục tiêu. */
    @ParameterizedTest
    @CsvSource({"30,5", "40,6", "50,8", "60,9", "70,10", "80,12", "90,13", "100,15", "110,16", "120,18", "130,19", "140,20"})
    void toDailyGoal_roundsUp(int weekly, int daily) {
        assertThat(ProgressGoalService.toDailyGoal(weekly)).isEqualTo(daily);
    }

    @Test
    void getMyGoal_noProfile_notConfigured_usesDefault() {
        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        ProgressGoalResponse res = service.getMyGoal();

        assertThat(res.isConfigured()).isFalse();
        assertThat(res.getWeeklyGoalWords()).isNull();
        assertThat(res.getDailyGoalWords()).isEqualTo(DailyStatsService.DEFAULT_GOAL_WORDS);
    }

    @Test
    void getMyGoal_profileWithoutGoal_notConfigured() {
        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(new UserProfile()));

        assertThat(service.getMyGoal().isConfigured()).isFalse();
    }

    @Test
    void getDailyGoal_usesChosenGoal() {
        UserProfile profile = new UserProfile();
        profile.setWeeklyGoalWords(50);
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));

        assertThat(service.getDailyGoal(USER_ID)).isEqualTo(8);
    }

    @Test
    void updateMyGoal_savesAndAppliesToToday() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(USER_ID);
        when(currentUserService.getCurrentUser()).thenReturn(user);
        UserProfile profile = new UserProfile();
        when(userService.getOrCreateProfile(user)).thenReturn(profile);

        ProgressGoalResponse res = service.updateMyGoal(60);

        assertThat(profile.getWeeklyGoalWords()).isEqualTo(60);
        verify(userProfileRepository).save(profile);
        verify(dailyStatsService).applyGoalToToday(USER_ID, 9);
        assertThat(res.isConfigured()).isTrue();
        assertThat(res.getDailyGoalWords()).isEqualTo(9);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 20, 35, 150, -30})
    void updateMyGoal_invalidValue_rejected(int weekly) {
        assertThatThrownBy(() -> service.updateMyGoal(weekly))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.PROGRESS_GOAL_INVALID);

        verify(userProfileRepository, never()).save(any());
        verify(dailyStatsService, never()).applyGoalToToday(any(), anyInt());
    }
}
