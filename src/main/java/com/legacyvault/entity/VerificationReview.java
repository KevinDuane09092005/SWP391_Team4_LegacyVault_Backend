package com.legacyvault.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "verification_reviews")
public class VerificationReview {

    @Id
    @Column(name = "review_id", length = 36)
    private String reviewId;

    @Column(name = "case_id", nullable = false, length = 36)
    private String caseId;

    @Column(name = "verifier_id", nullable = false, length = 36)
    private String verifierId;

    @Column(name = "decision", nullable = false, length = 50)
    private String decision; // APPROVED, REJECTED

    @Column(name = "rejection_reason_code", length = 50)
    private String rejectionReasonCode;

    @Column(name = "detailed_explanation", columnDefinition = "TEXT")
    private String detailedExplanation;

    @Column(name = "verifier_signature_payload", columnDefinition = "TEXT")
    private String verifierSignaturePayload;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    public VerificationReview() {
    }

    @PrePersist
    public void prePersist() {
        if (this.reviewId == null) {
            this.reviewId = java.util.UUID.randomUUID().toString();
        }
        if (this.reviewedAt == null) {
            this.reviewedAt = LocalDateTime.now();
        }
    }

    public String getReviewId() {
        return reviewId;
    }

    public void setReviewId(String reviewId) {
        this.reviewId = reviewId;
    }

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public String getVerifierId() {
        return verifierId;
    }

    public void setVerifierId(String verifierId) {
        this.verifierId = verifierId;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public String getRejectionReasonCode() {
        return rejectionReasonCode;
    }

    public void setRejectionReasonCode(String rejectionReasonCode) {
        this.rejectionReasonCode = rejectionReasonCode;
    }

    public String getDetailedExplanation() {
        return detailedExplanation;
    }

    public void setDetailedExplanation(String detailedExplanation) {
        this.detailedExplanation = detailedExplanation;
    }

    public String getVerifierSignaturePayload() {
        return verifierSignaturePayload;
    }

    public void setVerifierSignaturePayload(String verifierSignaturePayload) {
        this.verifierSignaturePayload = verifierSignaturePayload;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }
}
