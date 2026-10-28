package com.legacyvault.dto;

import com.legacyvault.entity.BillingCycle;
import com.legacyvault.entity.SubscriptionStatus;

import java.time.LocalDateTime;

public record SubscriptionResponse(
        String planId,
        BillingCycle billingCycle,
        SubscriptionStatus status,
        LocalDateTime startedAt,
        LocalDateTime currentPeriodEndsAt,
        LocalDateTime gracePeriodEndsAt,
        boolean autoRenew
) { }
