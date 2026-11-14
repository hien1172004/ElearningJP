package com.jp.elearningjp.entity.subscription;

import com.jp.elearningjp.entity.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_token_wallets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserTokenWallet {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder.Default
    @Column(name = "trial_balance", nullable = false)
    private Integer trialBalance = 3000;

    @Builder.Default
    @Column(name = "daily_free_balance", nullable = false)
    private Integer dailyFreeBalance = 500;

    @Builder.Default
    @Column(name = "subscription_balance", nullable = false)
    private Integer subscriptionBalance = 0;

    @Builder.Default
    @Column(name = "last_daily_reset", nullable = false)
    private LocalDate lastDailyReset = LocalDate.now();

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Tổng số token hiện có thể sử dụng (Trial + Daily + Subscription)
     */
    public int getTotalAvailableTokens() {
        return Math.max(0, trialBalance) + Math.max(0, dailyFreeBalance) + Math.max(0, subscriptionBalance);
    }
}

