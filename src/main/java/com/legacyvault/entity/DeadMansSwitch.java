package com.legacyvault.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "dead_mans_switches")
public class DeadMansSwitch {

    @Id
    @Column(name = "dms_id", length = 36)
    private String dmsId;

    @Column(name = "owner_id", nullable = false, unique = true, length = 36)
    private String ownerId;

    @Column(name = "check_in_interval_seconds", nullable = false)
    private int checkInIntervalSeconds = 30;

    @Column(name = "grace_period_seconds", nullable = false)
    private int gracePeriodSeconds = 50;

    @Column(name = "status", length = 50)
    private String status = "active";

    @Column(name = "last_check_in_at")
    private LocalDateTime lastCheckInAt;

    @Column(name = "next_check_in_at")
    private LocalDateTime nextCheckInAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public DeadMansSwitch() {
    }

    public DeadMansSwitch(String ownerId, int checkInIntervalSeconds, int gracePeriodSeconds) {
        this.ownerId = ownerId;
        this.checkInIntervalSeconds = checkInIntervalSeconds;
        this.gracePeriodSeconds = gracePeriodSeconds;
        this.status = "active";
        this.lastCheckInAt = LocalDateTime.now();
        this.nextCheckInAt = this.lastCheckInAt.plusSeconds(checkInIntervalSeconds);
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (this.dmsId == null) {
            this.dmsId = java.util.UUID.randomUUID().toString();
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
        if (this.lastCheckInAt == null) {
            this.lastCheckInAt = LocalDateTime.now();
        }
        if (this.nextCheckInAt == null) {
            this.nextCheckInAt = this.lastCheckInAt.plusSeconds(this.checkInIntervalSeconds);
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
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

    public int getCheckInIntervalSeconds() {
        return checkInIntervalSeconds;
    }

    public void setCheckInIntervalSeconds(int checkInIntervalSeconds) {
        this.checkInIntervalSeconds = checkInIntervalSeconds;
    }

    public int getGracePeriodSeconds() {
        return gracePeriodSeconds;
    }

    public void setGracePeriodSeconds(int gracePeriodSeconds) {
        this.gracePeriodSeconds = gracePeriodSeconds;
    }

    // Tương thích ngược với các hàm gọi dạng Days
    public int getCheckInIntervalDays() {
        return checkInIntervalSeconds;
    }

    public void setCheckInIntervalDays(int checkInIntervalDays) {
        this.checkInIntervalSeconds = checkInIntervalDays;
    }

    public int getGracePeriodDays() {
        return gracePeriodSeconds;
    }

    public void setGracePeriodDays(int gracePeriodDays) {
        this.gracePeriodSeconds = gracePeriodDays;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
