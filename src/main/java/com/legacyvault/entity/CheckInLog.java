package com.legacyvault.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "check_in_logs")
public class CheckInLog {

    @Id
    @Column(name = "log_id", length = 36)
    private String logId;

    @Column(name = "dms_id", nullable = false, length = 36)
    private String dmsId;

    @Column(name = "owner_id", nullable = false, length = 36)
    private String ownerId;

    @Column(name = "check_in_type", nullable = false, length = 50)
    private String checkInType;

    @Column(name = "status", length = 50)
    private String status = "verified";

    @Column(name = "recorded_at")
    private LocalDateTime recordedAt;

    public CheckInLog() {
    }

    public CheckInLog(String dmsId, String ownerId, String checkInType) {
        this.dmsId = dmsId;
        this.ownerId = ownerId;
        this.checkInType = checkInType;
        this.status = "verified";
        this.recordedAt = LocalDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (this.logId == null) {
            this.logId = java.util.UUID.randomUUID().toString();
        }
        if (this.recordedAt == null) {
            this.recordedAt = LocalDateTime.now();
        }
    }

    public String getLogId() {
        return logId;
    }

    public void setLogId(String logId) {
        this.logId = logId;
    }

    public String getDmsId() {
        return dmsId;
    }

    public void setDmsId(String dmsId) {
        this.dmsId = dmsId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getCheckInType() {
        return checkInType;
    }

    public void setCheckInType(String checkInType) {
        this.checkInType = checkInType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }
}
