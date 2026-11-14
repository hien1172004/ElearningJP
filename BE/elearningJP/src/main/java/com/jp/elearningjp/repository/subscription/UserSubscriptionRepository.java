package com.jp.elearningjp.repository.subscription;

import com.jp.elearningjp.entity.subscription.UserSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, UUID> {

    @Query("SELECT s FROM UserSubscription s " +
           "WHERE s.user.id = :userId AND s.status = 'ACTIVE' " +
           "AND (s.endAt IS NULL OR s.endAt > :now) " +
           "ORDER BY CASE WHEN s.plan.id = 'FREE' THEN 1 ELSE 0 END, s.createdAt DESC")
    java.util.List<UserSubscription> findActiveSubscriptions(@Param("userId") Long userId, @Param("now") Instant now);

    default Optional<UserSubscription> findActiveSubscription(Long userId, Instant now) {
        var list = findActiveSubscriptions(userId, now);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    Optional<UserSubscription> findTopByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);
}

