package com.legacyvault.dto;

public record BeneficiaryFaceCheckSessionResponse(
        String sessionId,
        String challengeDirection,
        int expiresInSeconds) {
}
