package com.jp.elearningjp.service.subscription;

import com.jp.elearningjp.dto.response.subscription.SubscriptionPlanResponse;
import com.jp.elearningjp.dto.response.subscription.UserSubscriptionResponse;
import com.jp.elearningjp.dto.response.subscription.UserTokenWalletResponse;
import com.jp.elearningjp.entity.subscription.Coupon;
import com.jp.elearningjp.entity.subscription.SubscriptionPlan;
import com.jp.elearningjp.entity.subscription.UserSubscription;
import com.jp.elearningjp.entity.subscription.UserTokenWallet;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.subscription.CouponRepository;
import com.jp.elearningjp.repository.subscription.SubscriptionPlanRepository;
import com.jp.elearningjp.repository.subscription.UserSubscriptionRepository;
import com.jp.elearningjp.repository.subscription.UserTokenWalletRepository;
import com.jp.elearningjp.repository.user.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SubscriptionService {

    SubscriptionPlanRepository planRepository;
    UserSubscriptionRepository userSubscriptionRepository;
    UserTokenWalletRepository tokenWalletRepository;
    CouponRepository couponRepository;
    UserRepository userRepository;

    /**
     * Khởi tạo gói FREE và Ví Token mặc định (3,000 token dùng thử + 500 token/ngày) cho user mới.
     */
    @Transactional
    public void initializeUserFreeTierAndWallet(User user) {
        if (user == null || user.getId() == null) return;

        SubscriptionPlan freePlan = planRepository.findById("FREE")
                .orElseGet(() -> planRepository.save(SubscriptionPlan.builder()
                        .id("FREE")
                        .name("Miễn phí")
                        .priceVnd(0)
                        .originalPriceVnd(0)
                        .durationDays(null)
                        .maxPracticeWords(10)
                        .isActive(true)
                        .build()));

        if (userSubscriptionRepository.findTopByUserIdAndStatusOrderByCreatedAtDesc(user.getId(), "ACTIVE").isEmpty()) {
            userSubscriptionRepository.save(UserSubscription.builder()
                    .user(user)
                    .plan(freePlan)
                    .status("ACTIVE")
                    .startAt(Instant.now())
                    .endAt(null)
                    .build());
        }

        if (tokenWalletRepository.findById(user.getId()).isEmpty()) {
            tokenWalletRepository.save(UserTokenWallet.builder()
                    .user(user)
                    .trialBalance(3000)
                    .dailyFreeBalance(500)
                    .subscriptionBalance(0)
                    .lastDailyReset(LocalDate.now())
                    .build());
        }
        log.info("Initialized Free Tier and Token Wallet for userId={}", user.getId());
    }

    /**
     * Kiểm tra nhanh xem người dùng có phải là Hội viên PRO (còn hạn hoặc trọn đời) hay không.
     */
    @Transactional(readOnly = true)
    public boolean isUserPro(Long userId) {
        if (userId == null) return false;
        return userSubscriptionRepository.findActiveSubscription(userId, Instant.now())
                .map(UserSubscription::isPro)
                .orElse(false);
    }

    /**
     * Lấy danh sách các gói dịch vụ đang mở bán công khai.
     */
    @Transactional(readOnly = true)
    public List<SubscriptionPlanResponse> getPublicPlans() {
        return planRepository.findByIsActiveTrueOrderByPriceVndAsc().stream()
                .map(this::toPlanResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy thông tin gói hiện tại của người dùng.
     */
    @Transactional(readOnly = true)
    public UserSubscriptionResponse getMySubscription(Long userId) {
        UserSubscription sub = userSubscriptionRepository.findActiveSubscription(userId, Instant.now())
                .orElseGet(() -> {
                    SubscriptionPlan freePlan = planRepository.findById("FREE").orElse(null);
                    return UserSubscription.builder()
                            .plan(freePlan)
                            .status("ACTIVE")
                            .startAt(Instant.now())
                            .build();
                });
        return toSubscriptionResponse(sub);
    }

    /**
     * Lấy trạng thái ví Token của người dùng (tự động reset 500 token nếu sang ngày mới).
     */
    @Transactional
    public UserTokenWalletResponse getMyWallet(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        UserTokenWallet wallet = tokenWalletRepository.findById(userId)
                .orElseGet(() -> {
                    UserTokenWallet newWallet = UserTokenWallet.builder()
                            .user(user)
                            .trialBalance(3000)
                            .dailyFreeBalance(500)
                            .subscriptionBalance(0)
                            .lastDailyReset(LocalDate.now())
                            .build();
                    return tokenWalletRepository.save(newWallet);
                });

        // Kiểm tra xem đã sang ngày mới chưa để reset daily token
        LocalDate today = LocalDate.now();
        if (wallet.getLastDailyReset() == null || wallet.getLastDailyReset().isBefore(today)) {
            wallet.setDailyFreeBalance(500);
            wallet.setLastDailyReset(today);
            wallet = tokenWalletRepository.save(wallet);
        }

        return toWalletResponse(wallet);
    }

    /**
     * Kiểm tra xem người dùng có đủ token để thực hiện tác vụ AI hay không.
     */
    @Transactional
    public boolean hasAvailableTokens(Long userId, int estimatedTokens) {
        if (isUserPro(userId)) {
            return true; // Hội viên PRO không bị giới hạn token
        }
        UserTokenWalletResponse wallet = getMyWallet(userId);
        return wallet.getTotalAvailableTokens() >= estimatedTokens;
    }

    /**
     * Trừ token của người dùng (ưu tiên trừ daily_free trước, sau đó tới trial_balance).
     */
    @Transactional
    public void deductTokens(Long userId, int tokensUsed) {
        if (tokensUsed <= 0 || isUserPro(userId)) {
            return; // PRO không cần trừ token
        }

        UserTokenWallet wallet = tokenWalletRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Lazy reset nếu qua ngày mới
        if (wallet.getLastDailyReset() == null || wallet.getLastDailyReset().isBefore(LocalDate.now())) {
            wallet.setDailyFreeBalance(500);
            wallet.setLastDailyReset(LocalDate.now());
        }

        int remaining = tokensUsed;

        // 1. Trừ vào dailyFreeBalance trước
        int daily = wallet.getDailyFreeBalance();
        if (daily > 0) {
            int deductFromDaily = Math.min(daily, remaining);
            wallet.setDailyFreeBalance(daily - deductFromDaily);
            remaining -= deductFromDaily;
        }

        // 2. Trừ tiếp vào subscriptionBalance nếu có
        if (remaining > 0) {
            int subBal = wallet.getSubscriptionBalance();
            if (subBal > 0) {
                int deductFromSub = Math.min(subBal, remaining);
                wallet.setSubscriptionBalance(subBal - deductFromSub);
                remaining -= deductFromSub;
            }
        }

        // 3. Trừ tiếp vào trialBalance dùng thử ban đầu
        if (remaining > 0) {
            int trial = wallet.getTrialBalance();
            int deductFromTrial = Math.min(trial, remaining);
            wallet.setTrialBalance(Math.max(0, trial - deductFromTrial));
        }

        tokenWalletRepository.save(wallet);
        log.info("Deducted {} tokens from userId={}, remaining wallet total={}",
                tokensUsed, userId, wallet.getTotalAvailableTokens());
    }

    /**
     * Kích hoạt gói PRO cho người dùng khi thanh toán thành công.
     */
    @Transactional
    public void activateProSubscription(Long userId, String planId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        SubscriptionPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found: " + planId));

        Instant now = Instant.now();
        Instant endAt = null;

        // Nếu người dùng đang có gói PRO còn hạn, cộng dồn thời gian
        var currentActiveOpt = userSubscriptionRepository.findActiveSubscription(userId, now);
        Instant baseStart = now;
        if (currentActiveOpt.isPresent() && currentActiveOpt.get().isPro() && currentActiveOpt.get().getEndAt() != null) {
            baseStart = currentActiveOpt.get().getEndAt();
        }

        if (plan.getDurationDays() != null) {
            endAt = baseStart.plus(plan.getDurationDays(), ChronoUnit.DAYS);
        }

        UserSubscription newSub = UserSubscription.builder()
                .user(user)
                .plan(plan)
                .status("ACTIVE")
                .startAt(now)
                .endAt(endAt)
                .build();

        userSubscriptionRepository.save(newSub);
        log.info("Activated PRO subscription [{}] for userId={}, expires at {}", planId, userId, endAt);
    }

    /**
     * Cron Job tự động reset 500 token hàng ngày lúc 00:00 (Asia/Ho_Chi_Minh).
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void resetDailyTokensCron() {
        log.info("Running daily token reset cron job...");
        tokenWalletRepository.findAll().forEach(w -> {
            if (w.getLastDailyReset() == null || w.getLastDailyReset().isBefore(LocalDate.now())) {
                w.setDailyFreeBalance(500);
                w.setLastDailyReset(LocalDate.now());
                tokenWalletRepository.save(w);
            }
        });
    }

    private SubscriptionPlanResponse toPlanResponse(SubscriptionPlan plan) {
        return SubscriptionPlanResponse.builder()
                .id(plan.getId())
                .name(plan.getName())
                .priceVnd(plan.getPriceVnd())
                .originalPriceVnd(plan.getOriginalPriceVnd())
                .durationDays(plan.getDurationDays())
                .monthlyTokens(plan.getMonthlyTokens())
                .maxPracticeWords(plan.getMaxPracticeWords())
                .isActive(plan.getIsActive())
                .build();
    }

    private UserSubscriptionResponse toSubscriptionResponse(UserSubscription sub) {
        return UserSubscriptionResponse.builder()
                .planId(sub.getPlan() != null ? sub.getPlan().getId() : "FREE")
                .planName(sub.getPlan() != null ? sub.getPlan().getName() : "Miễn phí")
                .status(sub.getStatus())
                .isPro(sub.isPro())
                .startAt(sub.getStartAt())
                .endAt(sub.getEndAt())
                .build();
    }

    private UserTokenWalletResponse toWalletResponse(UserTokenWallet wallet) {
        return UserTokenWalletResponse.builder()
                .trialBalance(wallet.getTrialBalance())
                .dailyFreeBalance(wallet.getDailyFreeBalance())
                .subscriptionBalance(wallet.getSubscriptionBalance())
                .totalAvailableTokens(wallet.getTotalAvailableTokens())
                .lastDailyReset(wallet.getLastDailyReset())
                .build();
    }
}

