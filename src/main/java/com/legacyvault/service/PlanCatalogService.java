package com.legacyvault.service;

import com.legacyvault.dto.PlanResponse;
import com.legacyvault.entity.BillingCycle;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PlanCatalogService {

    private final List<PlanResponse> plans = List.of(
            new PlanResponse("free", false, "USD",
                    new PlanResponse.Price(BigDecimal.ZERO, BigDecimal.ZERO),
                    new PlanResponse.Limits(1, 10, 1, 2)),
            new PlanResponse("essentials", false, "USD",
                    new PlanResponse.Price(new BigDecimal("4.99"), new BigDecimal("49.00")),
                    new PlanResponse.Limits(5, 50, 3, 10)),
            new PlanResponse("plus", true, "USD",
                    new PlanResponse.Price(new BigDecimal("8.99"), new BigDecimal("89.00")),
                    new PlanResponse.Limits(20, 250, 10, 25))
    );

    public List<PlanResponse> getPlans() {
        return plans;
    }

    public PlanResponse requirePlan(String planId) {
        return plans.stream().filter(plan -> plan.id().equals(planId)).findFirst()
                .orElseThrow(() -> BillingException.badRequest("UNKNOWN_PLAN", "Choose an available plan."));
    }

    public BigDecimal priceFor(PlanResponse plan, BillingCycle billingCycle) {
        return billingCycle == BillingCycle.YEARLY ? plan.price().yearly() : plan.price().monthly();
    }
}
