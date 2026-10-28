package com.legacyvault.repository;

import com.legacyvault.entity.UserSubscription;
import com.legacyvault.entity.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, String> {
    Optional<UserSubscription> findByUserId(String userId);
    List<UserSubscription> findByStatusAndCurrentPeriodEndsAtLessThanEqual(
            SubscriptionStatus status, LocalDateTime now);
    List<UserSubscription> findByStatusAndGracePeriodEndsAtLessThanEqual(
            SubscriptionStatus status, LocalDateTime now);
}
