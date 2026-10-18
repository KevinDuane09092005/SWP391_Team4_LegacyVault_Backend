package com.legacyvault.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class AuthorizationEvaluationResponse {

    @JsonProperty("evaluation_status")
    private String evaluationStatus;

    @JsonProperty("banner_notice")
    private String bannerNotice;

    @JsonProperty("checklist")
    private EvaluationChecklist checklist;

    @JsonProperty("authorization_results_summary")
    private String authorizationResultsSummary;

    @JsonProperty("authorization_results")
    private List<AssetVerdictItem> authorizationResults;

    @JsonProperty("execution_control")
    private ExecutionControlSummary executionControl;

    public AuthorizationEvaluationResponse() {}

    public AuthorizationEvaluationResponse(String evaluationStatus, String bannerNotice,
                                         EvaluationChecklist checklist, String authorizationResultsSummary,
                                         List<AssetVerdictItem> authorizationResults,
                                         ExecutionControlSummary executionControl) {
        this.evaluationStatus = evaluationStatus;
        this.bannerNotice = bannerNotice;
        this.checklist = checklist;
        this.authorizationResultsSummary = authorizationResultsSummary;
        this.authorizationResults = authorizationResults;
        this.executionControl = executionControl;
    }

    public String getEvaluationStatus() {
        return evaluationStatus;
    }

    public void setEvaluationStatus(String evaluationStatus) {
        this.evaluationStatus = evaluationStatus;
    }

    public String getBannerNotice() {
        return bannerNotice;
    }

    public void setBannerNotice(String bannerNotice) {
        this.bannerNotice = bannerNotice;
    }

    public EvaluationChecklist getChecklist() {
        return checklist;
    }

    public void setChecklist(EvaluationChecklist checklist) {
        this.checklist = checklist;
    }

    public String getAuthorizationResultsSummary() {
        return authorizationResultsSummary;
    }

    public void setAuthorizationResultsSummary(String authorizationResultsSummary) {
        this.authorizationResultsSummary = authorizationResultsSummary;
    }

    public List<AssetVerdictItem> getAuthorizationResults() {
        return authorizationResults;
    }

    public void setAuthorizationResults(List<AssetVerdictItem> authorizationResults) {
        this.authorizationResults = authorizationResults;
    }

    public ExecutionControlSummary getExecutionControl() {
        return executionControl;
    }

    public void setExecutionControl(ExecutionControlSummary executionControl) {
        this.executionControl = executionControl;
    }

    public static class EvaluationChecklist {
        @JsonProperty("beneficiary_identity_verified")
        private String beneficiaryIdentityVerified;

        @JsonProperty("certified_court_release_orders_verified")
        private String certifiedCourtReleaseOrdersVerified;

        @JsonProperty("cryptographic_handover_key_matching")
        private String cryptographicHandoverKeyMatching;

        public EvaluationChecklist() {}

        public EvaluationChecklist(String beneficiaryIdentityVerified, String certifiedCourtReleaseOrdersVerified, String cryptographicHandoverKeyMatching) {
            this.beneficiaryIdentityVerified = beneficiaryIdentityVerified;
            this.certifiedCourtReleaseOrdersVerified = certifiedCourtReleaseOrdersVerified;
            this.cryptographicHandoverKeyMatching = cryptographicHandoverKeyMatching;
        }

        public String getBeneficiaryIdentityVerified() { return beneficiaryIdentityVerified; }
        public void setBeneficiaryIdentityVerified(String v) { this.beneficiaryIdentityVerified = v; }
        public String getCertifiedCourtReleaseOrdersVerified() { return certifiedCourtReleaseOrdersVerified; }
        public void setCertifiedCourtReleaseOrdersVerified(String v) { this.certifiedCourtReleaseOrdersVerified = v; }
        public String getCryptographicHandoverKeyMatching() { return cryptographicHandoverKeyMatching; }
        public void setCryptographicHandoverKeyMatching(String v) { this.cryptographicHandoverKeyMatching = v; }
    }

    public static class AssetVerdictItem {
        @JsonProperty("asset_collection")
        private String assetCollection;

        @JsonProperty("type")
        private String type;

        @JsonProperty("verdict")
        private String verdict;

        public AssetVerdictItem() {}

        public AssetVerdictItem(String assetCollection, String type, String verdict) {
            this.assetCollection = assetCollection;
            this.type = type;
            this.verdict = verdict;
        }

        public String getAssetCollection() { return assetCollection; }
        public void setAssetCollection(String assetCollection) { this.assetCollection = assetCollection; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getVerdict() { return verdict; }
        public void setVerdict(String verdict) { this.verdict = verdict; }
    }

    public static class ExecutionControlSummary {
        @JsonProperty("clearance_status")
        private String clearanceStatus;

        @JsonProperty("authorized_by")
        private String authorizedBy;

        @JsonProperty("release_case")
        private String releaseCase;

        @JsonProperty("beneficiary")
        private String beneficiary;

        public ExecutionControlSummary() {}

        public ExecutionControlSummary(String clearanceStatus, String authorizedBy, String releaseCase, String beneficiary) {
            this.clearanceStatus = clearanceStatus;
            this.authorizedBy = authorizedBy;
            this.releaseCase = releaseCase;
            this.beneficiary = beneficiary;
        }

        public String getClearanceStatus() { return clearanceStatus; }
        public void setClearanceStatus(String clearanceStatus) { this.clearanceStatus = clearanceStatus; }
        public String getAuthorizedBy() { return authorizedBy; }
        public void setAuthorizedBy(String authorizedBy) { this.authorizedBy = authorizedBy; }
        public String getReleaseCase() { return releaseCase; }
        public void setReleaseCase(String releaseCase) { this.releaseCase = releaseCase; }
        public String getBeneficiary() { return beneficiary; }
        public void setBeneficiary(String beneficiary) { this.beneficiary = beneficiary; }
    }
}
