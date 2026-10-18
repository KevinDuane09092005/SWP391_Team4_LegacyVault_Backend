package com.legacyvault.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "handover_cases")
public class HandoverCase {

    @Id
    @Column(name = "case_id", length = 36)
    private String caseId;

    @Column(name = "case_number", nullable = false, unique = true, length = 50)
    private String caseNumber;

    @Column(name = "owner_id", nullable = false, length = 36)
    private String ownerId;

    @Column(name = "dms_id", nullable = false, length = 36)
    private String dmsId;

    @Column(name = "assigned_verifier_id", length = 36)
    private String assignedVerifierId;

    @Column(name = "status", length = 50)
    private String status = "initiated";

    @Column(name = "legal_verification_summary", length = 150)
    private String legalVerificationSummary;

    @Column(name = "clearance_status", length = 50)
    private String clearanceStatus = "PENDING";

    @Column(name = "authorized_by_executor", length = 100)
    private String authorizedByExecutor;

    @Column(name = "triggered_at")
    private LocalDateTime triggeredAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "vault_unlock_token")
    private String vaultUnlockToken;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    public HandoverCase() {
    }

    public HandoverCase(String caseNumber, String ownerId, String dmsId) {
        this.caseNumber = caseNumber;
        this.ownerId = ownerId;
        this.dmsId = dmsId;
        this.status = "initiated";
        this.triggeredAt = LocalDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (this.caseId == null) {
            this.caseId = java.util.UUID.randomUUID().toString();
        }
        if (this.triggeredAt == null) {
            this.triggeredAt = LocalDateTime.now();
        }
    }

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public String getCaseNumber() {
        return caseNumber;
    }

    public void setCaseNumber(String caseNumber) {
        this.caseNumber = caseNumber;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getDmsId() {
        return dmsId;
    }

    public void setDmsId(String dmsId) {
        this.dmsId = dmsId;
    }

    public String getAssignedVerifierId() {
        return assignedVerifierId;
    }

    public void setAssignedVerifierId(String assignedVerifierId) {
        this.assignedVerifierId = assignedVerifierId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLegalVerificationSummary() {
        return legalVerificationSummary;
    }

    public void setLegalVerificationSummary(String legalVerificationSummary) {
        this.legalVerificationSummary = legalVerificationSummary;
    }

    public String getClearanceStatus() {
        return clearanceStatus;
    }

    public void setClearanceStatus(String clearanceStatus) {
        this.clearanceStatus = clearanceStatus;
    }

    public String getAuthorizedByExecutor() {
        return authorizedByExecutor;
    }

    public void setAuthorizedByExecutor(String authorizedByExecutor) {
        this.authorizedByExecutor = authorizedByExecutor;
    }

    public LocalDateTime getTriggeredAt() {
        return triggeredAt;
    }

    public void setTriggeredAt(LocalDateTime triggeredAt) {
        this.triggeredAt = triggeredAt;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public String getVaultUnlockToken() {
        return vaultUnlockToken;
    }

    public void setVaultUnlockToken(String vaultUnlockToken) {
        this.vaultUnlockToken = vaultUnlockToken;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}
