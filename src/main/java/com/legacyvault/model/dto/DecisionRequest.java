package com.legacyvault.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DecisionRequest(
        @NotNull DecisionOutcome outcome,
        @NotBlank @Size(max = 2000) String reason
) {
}
