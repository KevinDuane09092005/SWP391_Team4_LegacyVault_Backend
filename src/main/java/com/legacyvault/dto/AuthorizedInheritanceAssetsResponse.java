package com.legacyvault.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class AuthorizedInheritanceAssetsResponse {

    @JsonProperty("welcome_user")
    private String welcomeUser;

    @JsonProperty("estate_owner")
    private String estateOwner;

    @JsonProperty("case_id")
    private String caseId;

    @JsonProperty("ready_for_handover")
    private List<ReadyAssetItem> readyForHandover;

    @JsonProperty("probate_restricted")
    private List<RestrictedAssetItem> probateRestricted;

    public AuthorizedInheritanceAssetsResponse() {}

    public AuthorizedInheritanceAssetsResponse(String welcomeUser, String estateOwner, String caseId,
                                              List<ReadyAssetItem> readyForHandover,
                                              List<RestrictedAssetItem> probateRestricted) {
        this.welcomeUser = welcomeUser;
        this.estateOwner = estateOwner;
        this.caseId = caseId;
        this.readyForHandover = readyForHandover;
        this.probateRestricted = probateRestricted;
    }

    public String getWelcomeUser() {
        return welcomeUser;
    }

    public void setWelcomeUser(String welcomeUser) {
        this.welcomeUser = welcomeUser;
    }

    public String getEstateOwner() {
        return estateOwner;
    }

    public void setEstateOwner(String estateOwner) {
        this.estateOwner = estateOwner;
    }

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public List<ReadyAssetItem> getReadyForHandover() {
        return readyForHandover;
    }

    public void setReadyForHandover(List<ReadyAssetItem> readyForHandover) {
        this.readyForHandover = readyForHandover;
    }

    public List<RestrictedAssetItem> getProbateRestricted() {
        return probateRestricted;
    }

    public void setProbateRestricted(List<RestrictedAssetItem> probateRestricted) {
        this.probateRestricted = probateRestricted;
    }

    public static class ReadyAssetItem {
        @JsonProperty("asset_id")
        private String assetId;

        @JsonProperty("name")
        private String name;

        @JsonProperty("category")
        private String category;

        @JsonProperty("release_protocols")
        private String releaseProtocols;

        @JsonProperty("handover_state")
        private String handoverState;

        @JsonProperty("action_type")
        private String actionType; // DECRYPT_AND_ACCESS, VIEW_DOCUMENT

        public ReadyAssetItem() {}

        public ReadyAssetItem(String assetId, String name, String category, String releaseProtocols, String handoverState, String actionType) {
            this.assetId = assetId;
            this.name = name;
            this.category = category;
            this.releaseProtocols = releaseProtocols;
            this.handoverState = handoverState;
            this.actionType = actionType;
        }

        public String getAssetId() { return assetId; }
        public void setAssetId(String assetId) { this.assetId = assetId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getReleaseProtocols() { return releaseProtocols; }
        public void setReleaseProtocols(String releaseProtocols) { this.releaseProtocols = releaseProtocols; }
        public String getHandoverState() { return handoverState; }
        public void setHandoverState(String handoverState) { this.handoverState = handoverState; }
        public String getActionType() { return actionType; }
        public void setActionType(String actionType) { this.actionType = actionType; }
    }

    public static class RestrictedAssetItem {
        @JsonProperty("name")
        private String name;

        @JsonProperty("category")
        private String category;

        @JsonProperty("reason_for_restriction")
        private String reasonForRestriction;

        @JsonProperty("status")
        private String status;

        @JsonProperty("is_accessible")
        private boolean isAccessible;

        public RestrictedAssetItem() {}

        public RestrictedAssetItem(String name, String category, String reasonForRestriction, String status, boolean isAccessible) {
            this.name = name;
            this.category = category;
            this.reasonForRestriction = reasonForRestriction;
            this.status = status;
            this.isAccessible = isAccessible;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getReasonForRestriction() { return reasonForRestriction; }
        public void setReasonForRestriction(String reasonForRestriction) { this.reasonForRestriction = reasonForRestriction; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public boolean isAccessible() { return isAccessible; }
        public void setAccessible(boolean accessible) { isAccessible = accessible; }
    }
}
