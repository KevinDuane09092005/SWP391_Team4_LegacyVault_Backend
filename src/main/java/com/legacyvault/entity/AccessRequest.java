package com.legacyvault.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;
import java.time.LocalDateTime;

@Entity
@Table(name = "access_requests")
public class AccessRequest {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "request_id", updatable = false, nullable = false, columnDefinition = "CHAR(36)")
    private String requestId;

    @Column(name = "requester_name", nullable = false, length = 100)
    private String requesterName;

    @Column(name = "requester_email", nullable = false, length = 150)
    private String requesterEmail;

    @Column(name = "requested_role", nullable = false, length = 50)
    private String requestedRole;

    @Column(name = "purpose", nullable = false, length = 255)
    private String purpose;

    @Column(name = "status", length = 50)
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED

    @Column(name = "reviewed_by", columnDefinition = "CHAR(36)")
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public AccessRequest() {}

    public AccessRequest(String requesterName, String requesterEmail, String requestedRole, String purpose) {
        this.requesterName = requesterName;
        this.requesterEmail = requesterEmail;
        this.requestedRole = requestedRole;
        this.purpose = purpose;
        this.status = "PENDING";
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public void setRequesterName(String requesterName) {
        this.requesterName = requesterName;
    }

    public String getRequesterEmail() {
        return requesterEmail;
    }

    public void setRequesterEmail(String requesterEmail) {
        this.requesterEmail = requesterEmail;
    }

    public String getRequestedRole() {
        return requestedRole;
    }

    public void setRequestedRole(String requestedRole) {
        this.requestedRole = requestedRole;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
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
        if (status == null) status = "PENDING";
    }
}
