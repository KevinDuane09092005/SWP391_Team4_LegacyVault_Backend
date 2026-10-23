package com.legacyvault.dto;

import java.time.LocalDateTime;

public class DmsStatusResponse {

    private String dmsId;
    private String ownerId;
    private String status;
    private int checkInIntervalSeconds;
    private int gracePeriodSeconds;
    private int checkInIntervalDays;
    private int gracePeriodDays;
    private LocalDateTime lastCheckInAt;
    private LocalDateTime nextCheckInAt;
    private long secondsRemaining;
    private long daysRemaining;
    private long gracePeriodSecondsRemaining;
    private long gracePeriodDaysRemaining;
    private String executorName;
    private String executorEmail;

    public DmsStatusResponse() {
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getCheckInIntervalSeconds() {
        return checkInIntervalSeconds > 0 ? checkInIntervalSeconds : checkInIntervalDays;
    }

    public void setCheckInIntervalSeconds(int checkInIntervalSeconds) {
        this.checkInIntervalSeconds = checkInIntervalSeconds;
        this.checkInIntervalDays = checkInIntervalSeconds;
    }

    public int getGracePeriodSeconds() {
        return gracePeriodSeconds > 0 ? gracePeriodSeconds : gracePeriodDays;
    }

    public void setGracePeriodSeconds(int gracePeriodSeconds) {
        this.gracePeriodSeconds = gracePeriodSeconds;
        this.gracePeriodDays = gracePeriodSeconds;
    }

    public int getCheckInIntervalDays() {
        return checkInIntervalSeconds > 0 ? checkInIntervalSeconds : checkInIntervalDays;
    }

    public void setCheckInIntervalDays(int checkInIntervalDays) {
        this.checkInIntervalDays = checkInIntervalDays;
        this.checkInIntervalSeconds = checkInIntervalDays;
    }

    public int getGracePeriodDays() {
        return gracePeriodSeconds > 0 ? gracePeriodSeconds : gracePeriodDays;
    }

    public void setGracePeriodDays(int gracePeriodDays) {
        this.gracePeriodDays = gracePeriodDays;
        this.gracePeriodSeconds = gracePeriodDays;
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

    public long getSecondsRemaining() {
        return secondsRemaining;
    }

    public void setSecondsRemaining(long secondsRemaining) {
        this.secondsRemaining = secondsRemaining;
    }

    public long getDaysRemaining() {
        return daysRemaining;
    }

    public void setDaysRemaining(long daysRemaining) {
        this.daysRemaining = daysRemaining;
    }

    public long getGracePeriodSecondsRemaining() {
        return gracePeriodSecondsRemaining;
    }

    public void setGracePeriodSecondsRemaining(long gracePeriodSecondsRemaining) {
        this.gracePeriodSecondsRemaining = gracePeriodSecondsRemaining;
    }

    public long getGracePeriodDaysRemaining() {
        return gracePeriodDaysRemaining;
    }

    public void setGracePeriodDaysRemaining(long gracePeriodDaysRemaining) {
        this.gracePeriodDaysRemaining = gracePeriodDaysRemaining;
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
}
