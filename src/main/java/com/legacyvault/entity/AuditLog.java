package com.legacyvault.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "log_id", updatable = false, nullable = false, columnDefinition = "CHAR(36)")
    private String logId;

    @Column(name = "user_id", columnDefinition = "CHAR(36)")
    private String userId;

    @Column(name = "user_name", length = 100)
    private String userName;

    @Column(name = "user_role", nullable = false, length = 50)
    private String userRole;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "target", length = 150)
    private String target;

    @Column(name = "status", length = 50)
    private String status = "SUCCESS";

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "previous_hash_sha256", length = 64)
    private String previousHashSha256;

    @Column(name = "current_hash_sha256", nullable = false, length = 64)
    private String currentHashSha256;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public AuditLog() {}

    public AuditLog(String userId, String userName, String userRole, String action, String target, String status, String ipAddress) {
        this.userId = userId;
        this.userName = userName;
        this.userRole = userRole;
        this.action = action;
        this.target = target;
        this.status = status != null ? status : "SUCCESS";
        this.ipAddress = ipAddress;
    }

    public String getLogId() {
        return logId;
    }

    public void setLogId(String logId) {
        this.logId = logId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserRole() {
        return userRole;
    }

    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getPreviousHashSha256() {
        return previousHashSha256;
    }

    public void setPreviousHashSha256(String previousHashSha256) {
        this.previousHashSha256 = previousHashSha256;
    }

    public String getCurrentHashSha256() {
        return currentHashSha256;
    }

    public void setCurrentHashSha256(String currentHashSha256) {
        this.currentHashSha256 = currentHashSha256;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = "SUCCESS";
        if (currentHashSha256 == null) {
            currentHashSha256 = "0a7f4e8b39c0e5a1d7f8c2b9a4e6f1c8d5b7a3e2f9c4d8a1b6e7f2c5d8a972bd";
        }
    }
}
