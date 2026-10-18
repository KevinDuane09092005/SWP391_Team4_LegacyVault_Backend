package com.legacyvault.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class FormalAcknowledgementSummaryResponse {

    @JsonProperty("case_id")
    private String caseId;

    @JsonProperty("step_title")
    private String stepTitle;

    @JsonProperty("status_badge")
    private String statusBadge;

    @JsonProperty("released_assets")
    private List<ReleasedAssetItem> releasedAssets;

    @JsonProperty("state_custody_trans_hash")
    private String stateCustodyTransHash;

    public FormalAcknowledgementSummaryResponse() {}

    public FormalAcknowledgementSummaryResponse(String caseId, String stepTitle, String statusBadge,
                                              List<ReleasedAssetItem> releasedAssets, String stateCustodyTransHash) {
        this.caseId = caseId;
        this.stepTitle = stepTitle;
        this.statusBadge = statusBadge;
        this.releasedAssets = releasedAssets;
        this.stateCustodyTransHash = stateCustodyTransHash;
    }

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public String getStepTitle() {
        return stepTitle;
    }

    public void setStepTitle(String stepTitle) {
        this.stepTitle = stepTitle;
    }

    public String getStatusBadge() {
        return statusBadge;
    }

    public void setStatusBadge(String statusBadge) {
        this.statusBadge = statusBadge;
    }

    public List<ReleasedAssetItem> getReleasedAssets() {
        return releasedAssets;
    }

    public void setReleasedAssets(List<ReleasedAssetItem> releasedAssets) {
        this.releasedAssets = releasedAssets;
    }

    public String getStateCustodyTransHash() {
        return stateCustodyTransHash;
    }

    public void setStateCustodyTransHash(String stateCustodyTransHash) {
        this.stateCustodyTransHash = stateCustodyTransHash;
    }

    public static class ReleasedAssetItem {
        @JsonProperty("asset_name")
        private String assetName;

        @JsonProperty("type_label")
        private String typeLabel;

        @JsonProperty("status")
        private String status;

        public ReleasedAssetItem() {}

        public ReleasedAssetItem(String assetName, String typeLabel, String status) {
            this.assetName = assetName;
            this.typeLabel = typeLabel;
            this.status = status;
        }

        public String getAssetName() { return assetName; }
        public void setAssetName(String assetName) { this.assetName = assetName; }
        public String getTypeLabel() { return typeLabel; }
        public void setTypeLabel(String typeLabel) { this.typeLabel = typeLabel; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
