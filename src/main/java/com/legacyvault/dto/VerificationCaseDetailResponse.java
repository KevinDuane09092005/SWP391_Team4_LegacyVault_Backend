package com.legacyvault.dto;

import java.util.List;

public class VerificationCaseDetailResponse {
    private String caseId;
    private String caseNumber;
    private String vaultOwnerName;
    private String vaultOwnerEmail;
    private String executorName;
    private String executorEmail;
    private String status;
    private String statusLabel;
    private String submittedDateFormatted;
    private String assignedVerifierName;
    private String reviewGuideline;
    private String rejectionReason;
    private List<VerificationDocumentResponse> documents;

    public VerificationCaseDetailResponse() {
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

    public String getVaultOwnerName() {
        return vaultOwnerName;
    }

    public void setVaultOwnerName(String vaultOwnerName) {
        this.vaultOwnerName = vaultOwnerName;
    }

    public String getVaultOwnerEmail() {
        return vaultOwnerEmail;
    }

    public void setVaultOwnerEmail(String vaultOwnerEmail) {
        this.vaultOwnerEmail = vaultOwnerEmail;
    }

    public String getExecutorName() {
        return executorName;
    }

    public void setExecutorName(String executorName) {
        this.executorName = executorName;
    }

    public String getExecutorEmail() {
        return executorEmail;
    }

    public void setExecutorEmail(String executorEmail) {
        this.executorEmail = executorEmail;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public void setStatusLabel(String statusLabel) {
        this.statusLabel = statusLabel;
    }

    public String getSubmittedDateFormatted() {
        return submittedDateFormatted;
    }

    public void setSubmittedDateFormatted(String submittedDateFormatted) {
        this.submittedDateFormatted = submittedDateFormatted;
    }

    public String getAssignedVerifierName() {
        return assignedVerifierName;
    }

    public void setAssignedVerifierName(String assignedVerifierName) {
        this.assignedVerifierName = assignedVerifierName;
    }

    public String getReviewGuideline() {
        return reviewGuideline;
    }

    public void setReviewGuideline(String reviewGuideline) {
        this.reviewGuideline = reviewGuideline;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public List<VerificationDocumentResponse> getDocuments() {
        return documents;
    }

    public void setDocuments(List<VerificationDocumentResponse> documents) {
        this.documents = documents;
    }
}
