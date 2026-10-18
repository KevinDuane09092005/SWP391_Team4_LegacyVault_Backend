package com.legacyvault.dto;

import java.time.Instant;

public record CaseDecisionResponse(
        DecisionOutcome outcome,
        String reason,
        String decidedByUserId,
        Instant decidedAt,
        String attestationType
) {
}
