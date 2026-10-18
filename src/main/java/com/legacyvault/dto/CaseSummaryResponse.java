package com.legacyvault.dto;

import com.legacyvault.entity.CaseStatus;

import java.time.Instant;

public record CaseSummaryResponse(
        String caseId,
        String handoverCaseId,
        String assignmentId,
        String beneficiaryContactId,
        String subjectDisplayName,
        String executorUserId,
        String reviewerUserId,
        CaseStatus status,
        Instant submittedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
