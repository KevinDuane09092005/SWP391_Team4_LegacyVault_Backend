package com.legacyvault.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;
import java.time.LocalDateTime;

@Entity
@Table(name = "assignment_beneficiaries")
public class AssignmentBeneficiary {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "split_id", updatable = false, nullable = false, columnDefinition = "CHAR(36)")
    private String splitId;

    @Column(name = "assignment_id", nullable = false, columnDefinition = "CHAR(36)")
    private String assignmentId;

    @Column(name = "beneficiary_contact_id", nullable = false, columnDefinition = "CHAR(36)")
    private String beneficiaryContactId;

    @Column(name = "status", length = 50)
    private String status = "pending"; // pending, notified, claimed, confirmed

    @Column(name = "release_status", length = 50)
    private String releaseStatus = "PENDING_VERIFICATION"; // AUTHORIZED & LOCKED, PENDING_VERIFICATION, AUTHORIZED FOR RELEASE, LOCKED / RESTRICTED

    @Column(name = "claimed_at")
    private LocalDateTime claimedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public AssignmentBeneficiary() {}

    public AssignmentBeneficiary(String assignmentId, String beneficiaryContactId) {
        this.assignmentId = assignmentId;
        this.beneficiaryContactId = beneficiaryContactId;
        this.status = "pending";
        this.releaseStatus = "PENDING_VERIFICATION";
    }

    public String getSplitId() {
        return splitId;
    }

    public void setSplitId(String splitId) {
        this.splitId = splitId;
    }

    public String getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(String assignmentId) {
        this.assignmentId = assignmentId;
    }

    public String getBeneficiaryContactId() {
        return beneficiaryContactId;
    }

    public void setBeneficiaryContactId(String beneficiaryContactId) {
        this.beneficiaryContactId = beneficiaryContactId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReleaseStatus() {
        return releaseStatus;
    }

    public void setReleaseStatus(String releaseStatus) {
        this.releaseStatus = releaseStatus;
    }

    public LocalDateTime getClaimedAt() {
        return claimedAt;
    }

    public void setClaimedAt(LocalDateTime claimedAt) {
        this.claimedAt = claimedAt;
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
        if (this.status == null) this.status = "pending";
        if (this.releaseStatus == null) this.releaseStatus = "PENDING_VERIFICATION";
    }
}
