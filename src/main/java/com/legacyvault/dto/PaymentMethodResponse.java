package com.legacyvault.dto;

import java.time.LocalDateTime;

public record PaymentMethodResponse(
        String paymentMethodId,
        String cardholderName,
        String brand,
        String lastFour,
        int expiryMonth,
        int expiryYear,
        BillingAddressRequest billingAddress,
        boolean isDefault,
        LocalDateTime createdAt
) { }
