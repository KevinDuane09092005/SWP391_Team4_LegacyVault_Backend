package com.legacyvault.dto;

public record CheckoutResponse(
        String result,
        PaymentTransactionResponse transaction,
        SubscriptionResponse subscription
) { }
