package com.legacyvault.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@Profile("!real-payments")
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public GatewayChargeResult charge(String providerPaymentMethodToken, BigDecimal amount,
                                      String currency, String transactionId) {
        if (providerPaymentMethodToken.endsWith("_declined")) {
            return new GatewayChargeResult(false, null, "DEMO_DECLINED",
                    "The demo payment was declined. Try a different demo payment method.");
        }
        return new GatewayChargeResult(true, "mock_tx_" + UUID.randomUUID(), null, null);
    }
}
