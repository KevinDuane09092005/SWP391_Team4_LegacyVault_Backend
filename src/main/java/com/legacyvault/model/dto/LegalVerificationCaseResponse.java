package com.legacyvault.model.dto;

import com.legacyvault.model.entity.CaseStatus;

import java.time.Instant;
import java.util.List;

public record LegalVerificationCaseResponse(
        String caseId,
        String activationId,
        String subjectDisplayName,
        String executorUserId,
        String reviewerUserId,
        CaseStatus status,
        Instant submittedAt,
        Instant createdAt,
        Instant updatedAt,
        CaseDecisionResponse decision,
        List<CaseDocumentResponse> documents
) {
}
