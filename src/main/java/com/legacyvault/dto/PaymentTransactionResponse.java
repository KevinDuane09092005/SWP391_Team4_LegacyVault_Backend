package com.legacyvault.dto;

import com.legacyvault.entity.BillingCycle;
import com.legacyvault.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentTransactionResponse(
        String transactionId,
        String planId,
        BillingCycle billingCycle,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String providerReference,
        String failureCode,
        String failureMessage,
        String paymentMethodBrand,
        String paymentMethodLastFour,
        LocalDateTime createdAt
) { }
