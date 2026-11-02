package com.legacyvault.dto;

public record BeneficiaryFaceVerificationResponse(
        String status,
        boolean passed,
        boolean challengePassed,
        double livenessScore,
        double antiSpoofScore,
        double faceMatchScore,
        String message,
        boolean demoOnly) {
}
