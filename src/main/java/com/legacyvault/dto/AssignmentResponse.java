package com.legacyvault.dto;

import java.time.LocalDateTime;
import java.util.List;

public class AssignmentResponse {

    private String assignmentId;
    private String assetId;
    private String assetName;
    private String assetType;
    private String executorContactId;
    private String executorName;
    private String executorEmail;
    private String handoverInstructions;
    private List<BeneficiaryDto> beneficiaries;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public AssignmentResponse() {}

    public AssignmentResponse(String assignmentId, String assetId, String assetName, String assetType, 
                              String executorContactId, String executorName, String executorEmail, 
                              String handoverInstructions, List<BeneficiaryDto> beneficiaries, 
                              LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.assignmentId = assignmentId;
        this.assetId = assetId;
        this.assetName = assetName;
        this.assetType = assetType;
        this.executorContactId = executorContactId;
        this.executorName = executorName;
        this.executorEmail = executorEmail;
        this.handoverInstructions = handoverInstructions;
        this.beneficiaries = beneficiaries;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    public String getAssetType() {
        return assetType;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

    public String getExecutorContactId() {
        return executorContactId;
    }

    public void setExecutorContactId(String executorContactId) {
        this.executorContactId = executorContactId;
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

    public String getHandoverInstructions() {
        return handoverInstructions;
    }

    public void setHandoverInstructions(String handoverInstructions) {
        this.handoverInstructions = handoverInstructions;
    }

    public List<BeneficiaryDto> getBeneficiaries() {
        return beneficiaries;
    }

    public void setBeneficiaries(List<BeneficiaryDto> beneficiaries) {
        this.beneficiaries = beneficiaries;
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
