package com.jp.elearningjp.repository.notification;

import com.jp.elearningjp.entity.notification.PushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {

    List<PushSubscription> findByUser_Id(Long userId);

    boolean existsByEndpoint(String endpoint);

    void deleteByEndpointAndUser_Id(String endpoint, Long userId);
}
