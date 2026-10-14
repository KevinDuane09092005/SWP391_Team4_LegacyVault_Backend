package com.legacyvault.dto;

import java.time.LocalDateTime;

public class AssetResponse {

    private String assetId;
    private String ownerId;
    private String assetType;
    private String assetName;
    private String description;
    private String decryptedCredentials;
    private String attachmentUrl;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public AssetResponse() {}

    public AssetResponse(String assetId, String ownerId, String assetType, String assetName, 
                         String description, String decryptedCredentials, String attachmentUrl, 
                         String status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.assetId = assetId;
        this.ownerId = ownerId;
        this.assetType = assetType;
        this.assetName = assetName;
        this.description = description;
        this.decryptedCredentials = decryptedCredentials;
        this.attachmentUrl = attachmentUrl;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getAssetType() {
        return assetType;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDecryptedCredentials() {
        return decryptedCredentials;
    }

    public void setDecryptedCredentials(String decryptedCredentials) {
        this.decryptedCredentials = decryptedCredentials;
    }

    public String getAttachmentUrl() {
        return attachmentUrl;
    }

    public void setAttachmentUrl(String attachmentUrl) {
        this.attachmentUrl = attachmentUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
