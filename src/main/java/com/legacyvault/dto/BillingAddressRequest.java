package com.legacyvault.dto;

import jakarta.validation.constraints.Size;

public record BillingAddressRequest(
        @Size(max = 200) String line1,
        @Size(max = 100) String city,
        @Size(max = 20) String postalCode,
        @Size(min = 2, max = 2) String country
) { }
