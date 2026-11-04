package com.legacyvault.dto;

import java.time.LocalDateTime;

public record BeneficiaryFaceEnrollmentStatusResponse(boolean enrolled, LocalDateTime consentedAt) {
}
