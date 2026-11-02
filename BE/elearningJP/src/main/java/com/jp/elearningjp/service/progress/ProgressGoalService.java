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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mục tiêu số từ mới mỗi tuần do user tự chọn.
 *
 * <p>Đánh giá "đạt mục tiêu" vẫn theo từng ngày, với mục tiêu ngày = {@code ceil(tuần / 7)}.</p>
 */
@Service
@RequiredArgsConstructor
public class ProgressGoalService {

    public static final int MIN_WEEKLY_GOAL = 30;
    public static final int MAX_WEEKLY_GOAL = 140;
    public static final int WEEKLY_GOAL_STEP = 10;

    private final UserProfileRepository userProfileRepository;
    private final UserService userService;
    private final DailyStatsService dailyStatsService;
    private final CurrentUserService currentUserService;

    /** Mục tiêu ngày tương ứng với mục tiêu tuần. Khớp công thức CEIL trong {@code UserDailyStatsRepository}. */
    public static int toDailyGoal(int weeklyGoalWords) {
        return (weeklyGoalWords + 6) / 7;
    }

    /** Mục tiêu ngày đang áp dụng của user (chưa chọn → mặc định). */
    @Transactional(readOnly = true)
    public int getDailyGoal(Long userId) {
        return userProfileRepository.findByUserId(userId)
                .map(UserProfile::getWeeklyGoalWords)
                .map(ProgressGoalService::toDailyGoal)
                .orElse(DailyStatsService.DEFAULT_GOAL_WORDS);
    }

    @Transactional(readOnly = true)
    public ProgressGoalResponse getMyGoal() {
        Integer weekly = userProfileRepository.findByUserId(currentUserService.getCurrentUserId())
                .map(UserProfile::getWeeklyGoalWords)
                .orElse(null);
        return toResponse(weekly);
    }

    /**
     * Đặt mục tiêu tuần và áp dụng ngay cho hôm nay. Các ngày trước giữ nguyên
     * vì mục tiêu đã được chốt trong {@code user_daily_stats.goal_words} của ngày đó.
     */
    @Transactional
    public ProgressGoalResponse updateMyGoal(int weeklyGoalWords) {
        if (weeklyGoalWords < MIN_WEEKLY_GOAL || weeklyGoalWords > MAX_WEEKLY_GOAL
                || weeklyGoalWords % WEEKLY_GOAL_STEP != 0) {
            throw new AppException(ErrorCode.PROGRESS_GOAL_INVALID);
        }

        User user = currentUserService.getCurrentUser();
        UserProfile profile = userService.getOrCreateProfile(user);
        profile.setWeeklyGoalWords(weeklyGoalWords);
        userProfileRepository.save(profile);

        dailyStatsService.applyGoalToToday(user.getId(), toDailyGoal(weeklyGoalWords));
        return toResponse(weeklyGoalWords);
    }

    private ProgressGoalResponse toResponse(Integer weekly) {
        return ProgressGoalResponse.builder()
                .configured(weekly != null)
                .weeklyGoalWords(weekly)
                .dailyGoalWords(weekly != null ? toDailyGoal(weekly) : DailyStatsService.DEFAULT_GOAL_WORDS)
                .build();
    }
}
