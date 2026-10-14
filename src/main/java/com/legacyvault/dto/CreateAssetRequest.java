package com.legacyvault.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateAssetRequest {

    @NotBlank(message = "Asset type is required")
    @Size(max = 50, message = "Asset type cannot exceed 50 characters")
    private String assetType;

    @NotBlank(message = "Asset name is required")
    @Size(max = 150, message = "Asset name cannot exceed 150 characters")
    private String assetName;

    private String description;

    private String credentials;

    private String attachmentUrl;

    public CreateAssetRequest() {}

    public CreateAssetRequest(String assetType, String assetName, String description, 
                              String credentials, String attachmentUrl) {
        this.assetType = assetType;
        this.assetName = assetName;
        this.description = description;
        this.credentials = credentials;
        this.attachmentUrl = attachmentUrl;
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

    public String getCredentials() {
        return credentials;
    }

    public void setCredentials(String credentials) {
        this.credentials = credentials;
    }

    public String getAttachmentUrl() {
        return attachmentUrl;
    }

    public void setAttachmentUrl(String attachmentUrl) {
        this.attachmentUrl = attachmentUrl;
    }
}
