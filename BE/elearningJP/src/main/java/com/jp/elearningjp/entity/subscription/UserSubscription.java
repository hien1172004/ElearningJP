package com.jp.elearningjp.entity.subscription;

import com.jp.elearningjp.entity.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_subscriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlan plan;

    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE"; // ACTIVE, EXPIRED, CANCELLED

    @Builder.Default
    @Column(name = "start_at", nullable = false)
    private Instant startAt = Instant.now();

    @Column(name = "end_at")
    private Instant endAt; // null for LIFETIME or FREE

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Kiểm tra xem gói hội viên còn hiệu lực hay không
     */
    public boolean isActive() {
        if (!"ACTIVE".equalsIgnoreCase(status)) {
            return false;
        }
        if (endAt == null) {
            return true; // Vĩnh viễn (LIFETIME hoặc FREE)
        }
        return Instant.now().isBefore(endAt);
    }

    /**
     * Kiểm tra xem có phải là gói PRO hay không
     */
    public boolean isPro() {
        if (!isActive()) return false;
        return plan != null && !"FREE".equalsIgnoreCase(plan.getId());
    }
}

