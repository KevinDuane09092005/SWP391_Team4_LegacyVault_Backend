package com.legacyvault.dto;

import java.time.LocalDateTime;

public class CheckInLogDto {

    private String logId;
    private String dmsId;
    private String checkInType;
    private String status;
    private LocalDateTime recordedAt;

    public CheckInLogDto() {
    }

    public CheckInLogDto(String logId, String checkInType, String status, LocalDateTime recordedAt) {
        this(logId, null, checkInType, status, recordedAt);
    }

    public CheckInLogDto(String logId, String dmsId, String checkInType, String status, LocalDateTime recordedAt) {
        this.logId = logId;
        this.dmsId = dmsId;
        this.checkInType = checkInType;
        this.status = status;
        this.recordedAt = recordedAt;
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
