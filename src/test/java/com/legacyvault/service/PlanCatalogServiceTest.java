package com.legacyvault.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlanCatalogServiceTest {

    @Test
    void planCatalogMatchesFrontendIdsPricesAndLimits() {
        PlanCatalogService catalog = new PlanCatalogService();

        assertEquals(3, catalog.getPlans().size());
        assertEquals("free", catalog.getPlans().get(0).id());
        assertEquals("4.99", catalog.requirePlan("essentials").price().monthly().toPlainString());
        assertEquals("49.00", catalog.requirePlan("essentials").price().yearly().toPlainString());
        assertEquals("plus", catalog.requirePlan("plus").id());
        assertEquals(250, catalog.requirePlan("plus").limits().records());
    }

    @Test
    void unknownPlanIsRejected() {
        PlanCatalogService catalog = new PlanCatalogService();
        assertThrows(BillingException.class, () -> catalog.requirePlan("custom-price"));
    }
}
