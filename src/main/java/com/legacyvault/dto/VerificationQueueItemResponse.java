package com.legacyvault.dto;

public class VerificationQueueItemResponse {
    private String caseId;
    private String caseNumber;
    private String vaultOwnerName;
    private String executorName;
    private String submittedDate;
    private String submittedDateFormatted;
    private String status;
    private String statusLabel;
    private int documentsCount;

    public VerificationQueueItemResponse() {
    }

    public VerificationQueueItemResponse(String caseId, String caseNumber, String vaultOwnerName,
                                         String executorName, String submittedDate, String submittedDateFormatted,
                                         String status, String statusLabel, int documentsCount) {
        this.caseId = caseId;
        this.caseNumber = caseNumber;
        this.vaultOwnerName = vaultOwnerName;
        this.executorName = executorName;
        this.submittedDate = submittedDate;
        this.submittedDateFormatted = submittedDateFormatted;
        this.status = status;
        this.statusLabel = statusLabel;
        this.documentsCount = documentsCount;
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

    public String getExecutorName() {
        return executorName;
    }

    public void setExecutorName(String executorName) {
        this.executorName = executorName;
    }

    public String getSubmittedDate() {
        return submittedDate;
    }

    public void setSubmittedDate(String submittedDate) {
        this.submittedDate = submittedDate;
    }

    public String getSubmittedDateFormatted() {
        return submittedDateFormatted;
    }

    public void setSubmittedDateFormatted(String submittedDateFormatted) {
        this.submittedDateFormatted = submittedDateFormatted;
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

    public int getDocumentsCount() {
        return documentsCount;
    }

    public void setDocumentsCount(int documentsCount) {
        this.documentsCount = documentsCount;
    }
}
