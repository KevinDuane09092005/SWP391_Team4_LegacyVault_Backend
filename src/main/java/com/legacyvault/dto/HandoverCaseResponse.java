package com.legacyvault.dto;

import java.time.LocalDateTime;

public class HandoverCaseResponse {

    private String caseId;
    private String caseNumber;
    private String ownerId;
    private String dmsId;
    private String status;
    private LocalDateTime triggeredAt;
    private LocalDateTime resolvedAt;

    public HandoverCaseResponse() {
    }

    public HandoverCaseResponse(String caseId, String caseNumber, String ownerId, String dmsId, String status, LocalDateTime triggeredAt, LocalDateTime resolvedAt) {
        this.caseId = caseId;
        this.caseNumber = caseNumber;
        this.ownerId = ownerId;
        this.dmsId = dmsId;
        this.status = status;
        this.triggeredAt = triggeredAt;
        this.resolvedAt = resolvedAt;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getTriggeredAt() {
        return triggeredAt;
    }

    public void setTriggeredAt(LocalDateTime triggeredAt) {
        this.triggeredAt = triggeredAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}
