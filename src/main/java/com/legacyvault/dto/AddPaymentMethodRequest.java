package com.legacyvault.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddPaymentMethodRequest(
        @NotBlank
        @Pattern(regexp = "^pm_mock_[A-Za-z0-9_-]{1,80}$")
        String providerPaymentMethodToken,
        @NotBlank @Size(max = 100) String cardholderName,
        @NotBlank @Pattern(regexp = "(?i)^(visa|mastercard|amex|other)$") String brand,
        @NotBlank @Pattern(regexp = "^[0-9]{4}$") String lastFour,
        @NotNull @Min(1) @Max(12) Integer expiryMonth,
        @NotNull @Min(2000) @Max(2100) Integer expiryYear,
        @Valid BillingAddressRequest billingAddress,
        boolean makeDefault
) { }
