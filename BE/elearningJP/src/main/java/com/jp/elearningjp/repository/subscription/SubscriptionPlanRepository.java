package com.jp.elearningjp.repository.subscription;

import com.jp.elearningjp.entity.subscription.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, String> {
    List<SubscriptionPlan> findByIsActiveTrueOrderByPriceVndAsc();
}

