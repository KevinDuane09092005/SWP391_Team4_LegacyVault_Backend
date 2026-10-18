package com.legacyvault.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;
import java.time.LocalDateTime;

@Entity
@Table(name = "asset_assignments")
public class AssetAssignment {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "assignment_id", updatable = false, nullable = false, columnDefinition = "CHAR(36)")
    private String assignmentId;

    @Column(name = "asset_id", nullable = false, unique = true, columnDefinition = "CHAR(36)")
    private String assetId;

    @Column(name = "executor_contact_id", nullable = false, columnDefinition = "CHAR(36)")
    private String executorContactId;

    @Column(name = "handover_protocol", length = 50)
    private String handoverProtocol = "legal_verification";

    @Column(name = "handover_instructions", columnDefinition = "TEXT")
    private String handoverInstructions;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public AssetAssignment() {}

    public AssetAssignment(String assetId, String executorContactId, String handoverInstructions) {
        this.assetId = assetId;
        this.executorContactId = executorContactId;
        this.handoverInstructions = handoverInstructions;
        this.handoverProtocol = "legal_verification";
    }

    public String getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(String assignmentId) {
        this.assignmentId = assignmentId;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public String getExecutorContactId() {
        return executorContactId;
    }

    public void setExecutorContactId(String executorContactId) {
        this.executorContactId = executorContactId;
    }

    public String getHandoverProtocol() {
        return handoverProtocol;
    }

    public void setHandoverProtocol(String handoverProtocol) {
        this.handoverProtocol = handoverProtocol;
    }

    public String getHandoverInstructions() {
        return handoverInstructions;
    }

    public void setHandoverInstructions(String handoverInstructions) {
        this.handoverInstructions = handoverInstructions;
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

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (this.handoverProtocol == null) {
            this.handoverProtocol = "legal_verification";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
