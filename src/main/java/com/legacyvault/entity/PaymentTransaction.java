package com.legacyvault.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transactions")
public class PaymentTransaction {

    @Id
    @Column(name = "transaction_id", nullable = false, updatable = false, length = 36,
            columnDefinition = "CHAR(36)")
    private String transactionId;

    @Column(name = "user_id", nullable = false, length = 36, columnDefinition = "CHAR(36)")
    private String userId;

    @Column(name = "plan_id", nullable = false, length = 24)
    private String planId;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false, length = 12)
    private BillingCycle billingCycle;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 12)
    private PaymentStatus status;

    @Column(name = "provider_transaction_reference", length = 128)
    private String providerTransactionReference;

    @Column(name = "failure_code", length = 48)
    private String failureCode;

    @Column(name = "failure_message", length = 255)
    private String failureMessage;

    @Column(name = "payment_method_brand", length = 24)
    private String paymentMethodBrand;

    @Column(name = "payment_method_last_four", length = 4)
    private String paymentMethodLastFour;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected PaymentTransaction() {
    }

    public PaymentTransaction(String transactionId, String userId, String planId, BillingCycle billingCycle,
                              BigDecimal amount, String currency, PaymentStatus status,
                              String providerTransactionReference, String failureCode,
                              String failureMessage, String paymentMethodBrand, String paymentMethodLastFour) {
        this.transactionId = transactionId;
        this.userId = userId;
        this.planId = planId;
        this.billingCycle = billingCycle;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.providerTransactionReference = providerTransactionReference;
        this.failureCode = failureCode;
        this.failureMessage = failureMessage;
        this.paymentMethodBrand = paymentMethodBrand;
        this.paymentMethodLastFour = paymentMethodLastFour;
    }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public String getTransactionId() { return transactionId; }
    public String getUserId() { return userId; }
    public String getPlanId() { return planId; }
    public BillingCycle getBillingCycle() { return billingCycle; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public PaymentStatus getStatus() { return status; }
    public String getProviderTransactionReference() { return providerTransactionReference; }
    public String getFailureCode() { return failureCode; }
    public String getFailureMessage() { return failureMessage; }
    public String getPaymentMethodBrand() { return paymentMethodBrand; }
    public String getPaymentMethodLastFour() { return paymentMethodLastFour; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
