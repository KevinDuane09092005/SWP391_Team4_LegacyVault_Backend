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

    @Column(name = "status", length = 50)
    private String status = "initiated";

    @Column(name = "triggered_at")
    private LocalDateTime triggeredAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

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
