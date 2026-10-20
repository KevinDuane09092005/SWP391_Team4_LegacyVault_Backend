package com.legacyvault.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class MockPaymentGatewayTest {

    private final MockPaymentGateway gateway = new MockPaymentGateway();

    @Test
    void tokenEndingInDeclinedReturnsDemoFailure() {
        var result = gateway.charge("pm_mock_card_declined", new BigDecimal("4.99"), "USD", "tx-1");

        assertFalse(result.approved());
        assertEquals("DEMO_DECLINED", result.failureCode());
        assertNull(result.providerReference());
    }

    @Test
    void otherMockTokenReturnsDemoSuccess() {
        var result = gateway.charge("pm_mock_card_4242", new BigDecimal("4.99"), "USD", "tx-2");

        assertTrue(result.approved());
        assertTrue(result.providerReference().startsWith("mock_tx_"));
        assertNull(result.failureCode());
    }
}
