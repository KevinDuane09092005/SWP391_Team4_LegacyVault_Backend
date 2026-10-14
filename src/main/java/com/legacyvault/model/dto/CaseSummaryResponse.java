package com.legacyvault.model.dto;

import com.legacyvault.model.entity.CaseStatus;

import java.time.Instant;

public record CaseSummaryResponse(
        String caseId,
        String activationId,
        String subjectDisplayName,
        String executorUserId,
        String reviewerUserId,
        CaseStatus status,
        Instant submittedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
