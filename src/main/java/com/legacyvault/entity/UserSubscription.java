package com.legacyvault.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_subscriptions")
public class UserSubscription {

    @Id
    @Column(name = "subscription_id", nullable = false, updatable = false, length = 36,
            columnDefinition = "CHAR(36)")
    private String subscriptionId;

    @Column(name = "user_id", nullable = false, unique = true, length = 36, columnDefinition = "CHAR(36)")
    private String userId;

    @Column(name = "plan_id", nullable = false, length = 24)
    private String planId;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false, length = 12)
    private BillingCycle billingCycle;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 12)
    private SubscriptionStatus status;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "current_period_ends_at")
    private LocalDateTime currentPeriodEndsAt;

    @Column(name = "grace_period_ends_at")
    private LocalDateTime gracePeriodEndsAt;

    @Column(name = "auto_renew", nullable = false)
    private boolean autoRenew;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private int version;

    protected UserSubscription() {
    }

    public UserSubscription(String subscriptionId, String userId, String planId, BillingCycle billingCycle,
                            SubscriptionStatus status, LocalDateTime startedAt,
                            LocalDateTime currentPeriodEndsAt, boolean autoRenew) {
        this.subscriptionId = subscriptionId;
        this.userId = userId;
        this.planId = planId;
        this.billingCycle = billingCycle;
        this.status = status;
        this.startedAt = startedAt;
        this.currentPeriodEndsAt = currentPeriodEndsAt;
        this.autoRenew = autoRenew;
    }

    @PrePersist
    void onCreate() { updatedAt = LocalDateTime.now(); }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }

    public String getSubscriptionId() { return subscriptionId; }
    public String getUserId() { return userId; }
    public String getPlanId() { return planId; }
    public BillingCycle getBillingCycle() { return billingCycle; }
    public SubscriptionStatus getStatus() { return status; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getCurrentPeriodEndsAt() { return currentPeriodEndsAt; }
    public LocalDateTime getGracePeriodEndsAt() { return gracePeriodEndsAt; }
    public boolean isAutoRenew() { return autoRenew; }

    public void activate(String planId, BillingCycle billingCycle, LocalDateTime startedAt,
                         LocalDateTime currentPeriodEndsAt) {
        activate(planId, billingCycle, startedAt, currentPeriodEndsAt, false);
    }

    public void activate(String planId, BillingCycle billingCycle, LocalDateTime startedAt,
                         LocalDateTime currentPeriodEndsAt, boolean autoRenew) {
        this.planId = planId;
        this.billingCycle = billingCycle;
        this.status = SubscriptionStatus.ACTIVE;
        this.startedAt = startedAt;
        this.currentPeriodEndsAt = currentPeriodEndsAt;
        this.gracePeriodEndsAt = null;
        this.autoRenew = autoRenew;
    }

    public void markPastDue(LocalDateTime gracePeriodEndsAt) {
        this.status = SubscriptionStatus.PAST_DUE;
        this.gracePeriodEndsAt = gracePeriodEndsAt;
    }

    public void cancelAutoRenewal() {
        this.autoRenew = false;
    }

    public void cancelAtPeriodEnd() {
        this.status = SubscriptionStatus.CANCELED;
        this.autoRenew = false;
        this.gracePeriodEndsAt = null;
    }

    public void completeRenewal(LocalDateTime currentPeriodEndsAt) {
        this.status = SubscriptionStatus.ACTIVE;
        this.currentPeriodEndsAt = currentPeriodEndsAt;
        this.gracePeriodEndsAt = null;
        this.autoRenew = true;
    }

    public void expire() {
        this.status = SubscriptionStatus.EXPIRED;
        this.autoRenew = false;
        this.gracePeriodEndsAt = null;
    }
}
