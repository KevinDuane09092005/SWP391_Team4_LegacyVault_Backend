package com.legacyvault.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class HandoverDetailsResponse {

    @JsonProperty("case_number")
    private String caseNumber;

    @JsonProperty("handover_status")
    private String handoverStatus;

    @JsonProperty("vault_owner")
    private VaultOwnerSummary vaultOwner;

    @JsonProperty("primary_beneficiary")
    private BeneficiarySummary primaryBeneficiary;

    @JsonProperty("legal_verification")
    private LegalVerificationSummary legalVerification;

    @JsonProperty("authorized_assets")
    private List<HandoverAssetItem> authorizedAssets;

    public HandoverDetailsResponse() {}

    public HandoverDetailsResponse(String caseNumber, String handoverStatus, VaultOwnerSummary vaultOwner,
                                   BeneficiarySummary primaryBeneficiary, LegalVerificationSummary legalVerification,
                                   List<HandoverAssetItem> authorizedAssets) {
        this.caseNumber = caseNumber;
        this.handoverStatus = handoverStatus;
        this.vaultOwner = vaultOwner;
        this.primaryBeneficiary = primaryBeneficiary;
        this.legalVerification = legalVerification;
        this.authorizedAssets = authorizedAssets;
    }

    public String getCaseNumber() {
        return caseNumber;
    }

    public void setCaseNumber(String caseNumber) {
        this.caseNumber = caseNumber;
    }

    public String getHandoverStatus() {
        return handoverStatus;
    }

    public void setHandoverStatus(String handoverStatus) {
        this.handoverStatus = handoverStatus;
    }

    public VaultOwnerSummary getVaultOwner() {
        return vaultOwner;
    }

    public void setVaultOwner(VaultOwnerSummary vaultOwner) {
        this.vaultOwner = vaultOwner;
    }

    public BeneficiarySummary getPrimaryBeneficiary() {
        return primaryBeneficiary;
    }

    public void setPrimaryBeneficiary(BeneficiarySummary primaryBeneficiary) {
        this.primaryBeneficiary = primaryBeneficiary;
    }

    public LegalVerificationSummary getLegalVerification() {
        return legalVerification;
    }

    public void setLegalVerification(LegalVerificationSummary legalVerification) {
        this.legalVerification = legalVerification;
    }

    public List<HandoverAssetItem> getAuthorizedAssets() {
        return authorizedAssets;
    }

    public void setAuthorizedAssets(List<HandoverAssetItem> authorizedAssets) {
        this.authorizedAssets = authorizedAssets;
    }

    public static class VaultOwnerSummary {
        @JsonProperty("name")
        private String name;

        @JsonProperty("email")
        private String email;

        @JsonProperty("status")
        private String status;

        public VaultOwnerSummary() {}

        public VaultOwnerSummary(String name, String email, String status) {
            this.name = name;
            this.email = email;
            this.status = status;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public static class BeneficiarySummary {
        @JsonProperty("name")
        private String name;

        @JsonProperty("email")
        private String email;

        public BeneficiarySummary() {}

        public BeneficiarySummary(String name, String email) {
            this.name = name;
            this.email = email;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    public static class LegalVerificationSummary {
        @JsonProperty("status")
        private String status;

        @JsonProperty("verification_note")
        private String verificationNote;

        public LegalVerificationSummary() {}

        public LegalVerificationSummary(String status, String verificationNote) {
            this.status = status;
            this.verificationNote = verificationNote;
        }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getVerificationNote() { return verificationNote; }
        public void setVerificationNote(String verificationNote) { this.verificationNote = verificationNote; }
    }

    public static class HandoverAssetItem {
        @JsonProperty("asset_id")
        private String assetId;

        @JsonProperty("asset_name")
        private String assetName;

        @JsonProperty("category")
        private String category;

        @JsonProperty("description")
        private String description;

        @JsonProperty("credentials")
        private String credentials;

        @JsonProperty("attachment_url")
        private String attachmentUrl;

        @JsonProperty("handover_protocol")
        private String handoverProtocol;

        @JsonProperty("release_status")
        private String releaseStatus;

        public HandoverAssetItem() {}

        public HandoverAssetItem(String assetName, String category, String handoverProtocol, String releaseStatus) {
            this.assetName = assetName;
            this.category = category;
            this.handoverProtocol = handoverProtocol;
            this.releaseStatus = releaseStatus;
        }

        public HandoverAssetItem(String assetId, String assetName, String category, String description,
                                 String credentials, String attachmentUrl, String handoverProtocol, String releaseStatus) {
            this.assetId = assetId;
            this.assetName = assetName;
            this.category = category;
            this.description = description;
            this.credentials = credentials;
            this.attachmentUrl = attachmentUrl;
            this.handoverProtocol = handoverProtocol;
            this.releaseStatus = releaseStatus;
        }

        public String getAssetId() { return assetId; }
        public void setAssetId(String assetId) { this.assetId = assetId; }
        public String getAssetName() { return assetName; }
        public void setAssetName(String assetName) { this.assetName = assetName; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getCredentials() { return credentials; }
        public void setCredentials(String credentials) { this.credentials = credentials; }
        public String getAttachmentUrl() { return attachmentUrl; }
        public void setAttachmentUrl(String attachmentUrl) { this.attachmentUrl = attachmentUrl; }
        public String getHandoverProtocol() { return handoverProtocol; }
        public void setHandoverProtocol(String handoverProtocol) { this.handoverProtocol = handoverProtocol; }
        public String getReleaseStatus() { return releaseStatus; }
        public void setReleaseStatus(String releaseStatus) { this.releaseStatus = releaseStatus; }
    }
}
