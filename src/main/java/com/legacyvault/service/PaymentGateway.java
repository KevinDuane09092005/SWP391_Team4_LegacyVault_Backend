package com.legacyvault.service;

import java.math.BigDecimal;

public interface PaymentGateway {
    GatewayChargeResult charge(String providerPaymentMethodToken, BigDecimal amount,
                               String currency, String transactionId);

    record GatewayChargeResult(boolean approved, String providerReference,
                               String failureCode, String failureMessage) { }
}
