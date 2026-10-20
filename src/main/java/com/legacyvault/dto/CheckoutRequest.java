package com.legacyvault.dto;

import com.legacyvault.entity.BillingCycle;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CheckoutRequest(
        @NotBlank @Pattern(regexp = "^(free|essentials|plus)$") String planId,
        @NotNull BillingCycle billingCycle,
        String paymentMethodId,
        Boolean acceptRecurringBilling
) { }
