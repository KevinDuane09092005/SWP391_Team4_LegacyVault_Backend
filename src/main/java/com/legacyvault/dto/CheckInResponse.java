package com.legacyvault.dto;

import java.time.LocalDateTime;

public class CheckInResponse {

    private boolean success;
    private String message;
    private String status;
    private LocalDateTime lastCheckInAt;
    private LocalDateTime nextCheckInAt;

    public CheckInResponse() {
    }

    public CheckInResponse(boolean success, String message, String status, LocalDateTime lastCheckInAt, LocalDateTime nextCheckInAt) {
        this.success = success;
        this.message = message;
        this.status = status;
        this.lastCheckInAt = lastCheckInAt;
        this.nextCheckInAt = nextCheckInAt;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getLastCheckInAt() {
        return lastCheckInAt;
    }

    public void setLastCheckInAt(LocalDateTime lastCheckInAt) {
        this.lastCheckInAt = lastCheckInAt;
    }

    public LocalDateTime getNextCheckInAt() {
        return nextCheckInAt;
    }

    public void setNextCheckInAt(LocalDateTime nextCheckInAt) {
        this.nextCheckInAt = nextCheckInAt;
    }
}
