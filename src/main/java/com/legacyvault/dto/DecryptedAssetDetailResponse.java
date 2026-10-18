package com.legacyvault.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class DecryptedAssetDetailResponse {

    @JsonProperty("asset_id")
    private String assetId;

    @JsonProperty("asset_name")
    private String assetName;

    @JsonProperty("vault_subtitle")
    private String vaultSubtitle;

    @JsonProperty("security_badge")
    private String securityBadge;

    @JsonProperty("secure_access_instructions")
    private SecureAccessInstructions secureAccessInstructions;

    @JsonProperty("decrypted_credential_data")
    private DecryptedCredentialData decryptedCredentialData;

    @JsonProperty("cryptographic_integrity_audit_log")
    private List<String> cryptographicIntegrityAuditLog;

    @JsonProperty("handover_metadata")
    private HandoverMetadata handoverMetadata;

    @JsonProperty("download_recovery_credential_pdf_url")
    private String downloadRecoveryCredentialPdfUrl;

    public DecryptedAssetDetailResponse() {}

    public DecryptedAssetDetailResponse(String assetId, String assetName, String vaultSubtitle, String securityBadge,
                                        SecureAccessInstructions secureAccessInstructions,
                                        DecryptedCredentialData decryptedCredentialData,
                                        List<String> cryptographicIntegrityAuditLog,
                                        HandoverMetadata handoverMetadata,
                                        String downloadRecoveryCredentialPdfUrl) {
        this.assetId = assetId;
        this.assetName = assetName;
        this.vaultSubtitle = vaultSubtitle;
        this.securityBadge = securityBadge;
        this.secureAccessInstructions = secureAccessInstructions;
        this.decryptedCredentialData = decryptedCredentialData;
        this.cryptographicIntegrityAuditLog = cryptographicIntegrityAuditLog;
        this.handoverMetadata = handoverMetadata;
        this.downloadRecoveryCredentialPdfUrl = downloadRecoveryCredentialPdfUrl;
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

    public String getVaultSubtitle() {
        return vaultSubtitle;
    }

    public void setVaultSubtitle(String vaultSubtitle) {
        this.vaultSubtitle = vaultSubtitle;
    }

    public String getSecurityBadge() {
        return securityBadge;
    }

    public void setSecurityBadge(String securityBadge) {
        this.securityBadge = securityBadge;
    }

    public SecureAccessInstructions getSecureAccessInstructions() {
        return secureAccessInstructions;
    }

    public void setSecureAccessInstructions(SecureAccessInstructions secureAccessInstructions) {
        this.secureAccessInstructions = secureAccessInstructions;
    }

    public DecryptedCredentialData getDecryptedCredentialData() {
        return decryptedCredentialData;
    }

    public void setDecryptedCredentialData(DecryptedCredentialData decryptedCredentialData) {
        this.decryptedCredentialData = decryptedCredentialData;
    }

    public List<String> getCryptographicIntegrityAuditLog() {
        return cryptographicIntegrityAuditLog;
    }

    public void setCryptographicIntegrityAuditLog(List<String> cryptographicIntegrityAuditLog) {
        this.cryptographicIntegrityAuditLog = cryptographicIntegrityAuditLog;
    }

    public HandoverMetadata getHandoverMetadata() {
        return handoverMetadata;
    }

    public void setHandoverMetadata(HandoverMetadata handoverMetadata) {
        this.handoverMetadata = handoverMetadata;
    }

    public String getDownloadRecoveryCredentialPdfUrl() {
        return downloadRecoveryCredentialPdfUrl;
    }

    public void setDownloadRecoveryCredentialPdfUrl(String downloadRecoveryCredentialPdfUrl) {
        this.downloadRecoveryCredentialPdfUrl = downloadRecoveryCredentialPdfUrl;
    }

    public static class SecureAccessInstructions {
        @JsonProperty("quote")
        private String quote;

        @JsonProperty("author")
        private String author;

        public SecureAccessInstructions() {}

        public SecureAccessInstructions(String quote, String author) {
            this.quote = quote;
            this.author = author;
        }

        public String getQuote() { return quote; }
        public void setQuote(String quote) { this.quote = quote; }
        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
    }

    public static class DecryptedCredentialData {
        @JsonProperty("label")
        private String label;

        @JsonProperty("passphrase")
        private String passphrase;

        @JsonProperty("access_url")
        private String accessUrl;

        public DecryptedCredentialData() {}

        public DecryptedCredentialData(String label, String passphrase, String accessUrl) {
            this.label = label;
            this.passphrase = passphrase;
            this.accessUrl = accessUrl;
        }

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public String getPassphrase() { return passphrase; }
        public void setPassphrase(String passphrase) { this.passphrase = passphrase; }
        public String getAccessUrl() { return accessUrl; }
        public void setAccessUrl(String accessUrl) { this.accessUrl = accessUrl; }
    }

    public static class HandoverMetadata {
        @JsonProperty("case_id")
        private String caseId;

        @JsonProperty("former_custodian")
        private String formerCustodian;

        @JsonProperty("authorized_recipient")
        private String authorizedRecipient;

        public HandoverMetadata() {}

        public HandoverMetadata(String caseId, String formerCustodian, String authorizedRecipient) {
            this.caseId = caseId;
            this.formerCustodian = formerCustodian;
            this.authorizedRecipient = authorizedRecipient;
        }

        public String getCaseId() { return caseId; }
        public void setCaseId(String caseId) { this.caseId = caseId; }
        public String getFormerCustodian() { return formerCustodian; }
        public void setFormerCustodian(String formerCustodian) { this.formerCustodian = formerCustodian; }
        public String getAuthorizedRecipient() { return authorizedRecipient; }
        public void setAuthorizedRecipient(String authorizedRecipient) { this.authorizedRecipient = authorizedRecipient; }
    }
}
