package com.legacyvault.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

@Entity
@Table(name = "legal_verification_cases")
public class LegalVerificationCase {

    @Id
    @Column(name = "case_id", length = 36, columnDefinition = "CHAR(36)")
    private String caseId;

    @Column(name = "activation_id", nullable = false, length = 36, columnDefinition = "CHAR(36)")
    private String activationId;

    @Column(name = "executor_user_id", nullable = false, length = 36, columnDefinition = "CHAR(36)")
    private String executorUserId;

    @Column(name = "reviewer_user_id", length = 36, columnDefinition = "CHAR(36)")
    private String reviewerUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private CaseStatus status = CaseStatus.DRAFT;

    @Column(name = "decision_reason", columnDefinition = "TEXT")
    private String decisionReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision_attestation_type", length = 40)
    private AttestationType decisionAttestationType;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private int version;

    protected LegalVerificationCase() {
    }

    public LegalVerificationCase(String caseId, String activationId, String executorUserId) {
        this.caseId = caseId;
        this.activationId = activationId;
        this.executorUserId = executorUserId;
        this.status = CaseStatus.DRAFT;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public String getCaseId() { return caseId; }
    public String getActivationId() { return activationId; }
    public String getExecutorUserId() { return executorUserId; }
    public String getReviewerUserId() { return reviewerUserId; }
    public void setReviewerUserId(String reviewerUserId) { this.reviewerUserId = reviewerUserId; }
    public CaseStatus getStatus() { return status; }
    public void setStatus(CaseStatus status) { this.status = status; }
    public String getDecisionReason() { return decisionReason; }
    public void setDecisionReason(String decisionReason) { this.decisionReason = decisionReason; }
    public AttestationType getDecisionAttestationType() { return decisionAttestationType; }
    public void setDecisionAttestationType(AttestationType type) { this.decisionAttestationType = type; }
    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
    public Instant getDecidedAt() { return decidedAt; }
    public void setDecidedAt(Instant decidedAt) { this.decidedAt = decidedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public int getVersion() { return version; }
}
