package com.legacyvault.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateAutoRenewRequest(@NotNull Boolean autoRenew) { }
