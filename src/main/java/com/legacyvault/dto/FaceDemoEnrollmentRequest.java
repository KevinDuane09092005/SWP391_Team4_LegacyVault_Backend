package com.legacyvault.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record FaceDemoEnrollmentRequest(
        boolean consent,
        @NotEmpty @Size(min = 64, max = 4096) List<@NotNull Double> embedding) {
}
