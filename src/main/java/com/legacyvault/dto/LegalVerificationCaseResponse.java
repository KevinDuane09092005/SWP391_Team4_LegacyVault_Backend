package com.legacyvault.dto;

import com.legacyvault.entity.CaseStatus;

import java.time.Instant;
import java.util.List;

public record LegalVerificationCaseResponse(
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
        Instant updatedAt,
        CaseDecisionResponse decision,
        List<CaseDocumentResponse> documents
) {
}
