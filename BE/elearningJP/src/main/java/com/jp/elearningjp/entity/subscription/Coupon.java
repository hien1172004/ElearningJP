package com.jp.elearningjp.entity.subscription;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "coupons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "discount_type", nullable = false, length = 20)
    private String discountType; // 'PERCENTAGE' or 'FIXED'

    @Column(name = "discount_value", nullable = false)
    private Integer discountValue;

    @Column(name = "max_discount_amount")
    private Integer maxDiscountAmount;

    @Builder.Default
    @Column(name = "min_order_amount", nullable = false)
    private Integer minOrderAmount = 0;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Builder.Default
    @Column(name = "used_count", nullable = false)
    private Integer usedCount = 0;

    @Column(name = "start_at")
    private Instant startAt;

    @Column(name = "end_at")
    private Instant endAt;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Kiểm tra xem mã giảm giá hiện có hợp lệ hay không
     */
    public boolean isValid(int orderAmount) {
        if (!Boolean.TRUE.equals(isActive)) return false;
        Instant now = Instant.now();
        if (startAt != null && now.isBefore(startAt)) return false;
        if (endAt != null && now.isAfter(endAt)) return false;
        if (usageLimit != null && usedCount >= usageLimit) return false;
        return orderAmount >= minOrderAmount;
    }

    /**
     * Tính toán số tiền được giảm
     */
    public int calculateDiscount(int orderAmount) {
        if (!isValid(orderAmount)) return 0;
        int discount;
        if ("PERCENTAGE".equalsIgnoreCase(discountType)) {
            discount = (orderAmount * discountValue) / 100;
            if (maxDiscountAmount != null && discount > maxDiscountAmount) {
                discount = maxDiscountAmount;
            }
        } else {
            discount = discountValue;
        }
        return Math.min(discount, orderAmount);
    }
}

