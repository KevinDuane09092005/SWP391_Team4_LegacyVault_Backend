package com.legacyvault.dto;

import java.time.LocalDateTime;

public class ApproveCaseResponse {
    private String caseId;
    private String caseNumber;
    private String status;
    private String statusLabel;
    private String approvedTitle;
    private String approvedMessage;
    private Boolean vaultUnlockAuthorized;
    private String vaultUnlockToken;
    private LocalDateTime verifiedAt;

    public ApproveCaseResponse() {
    }

    public ApproveCaseResponse(String caseId, String caseNumber, String status, String statusLabel,
                               String approvedTitle, String approvedMessage, Boolean vaultUnlockAuthorized,
                               String vaultUnlockToken, LocalDateTime verifiedAt) {
        this.caseId = caseId;
        this.caseNumber = caseNumber;
        this.status = status;
        this.statusLabel = statusLabel;
        this.approvedTitle = approvedTitle;
        this.approvedMessage = approvedMessage;
        this.vaultUnlockAuthorized = vaultUnlockAuthorized;
        this.vaultUnlockToken = vaultUnlockToken;
        this.verifiedAt = verifiedAt;
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

    public String getApprovedTitle() {
        return approvedTitle;
    }

    public void setApprovedTitle(String approvedTitle) {
        this.approvedTitle = approvedTitle;
    }

    public String getApprovedMessage() {
        return approvedMessage;
    }

    public void setApprovedMessage(String approvedMessage) {
        this.approvedMessage = approvedMessage;
    }

    public Boolean getVaultUnlockAuthorized() {
        return vaultUnlockAuthorized;
    }

    public void setVaultUnlockAuthorized(Boolean vaultUnlockAuthorized) {
        this.vaultUnlockAuthorized = vaultUnlockAuthorized;
    }

    public String getVaultUnlockToken() {
        return vaultUnlockToken;
    }

    public void setVaultUnlockToken(String vaultUnlockToken) {
        this.vaultUnlockToken = vaultUnlockToken;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }
}
