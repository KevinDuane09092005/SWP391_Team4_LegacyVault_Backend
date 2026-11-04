package com.legacyvault.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record FaceDemoVerificationRequest(
        boolean challengeCompleted,
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0") Double livenessScore,
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0") Double antiSpoofScore,
        @NotEmpty @Size(min = 64, max = 4096) List<@NotNull Double> embedding) {
}
