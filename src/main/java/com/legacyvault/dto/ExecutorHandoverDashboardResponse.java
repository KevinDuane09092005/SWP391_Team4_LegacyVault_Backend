package com.legacyvault.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class ExecutorHandoverDashboardResponse {

    @JsonProperty("metrics")
    private DashboardMetrics metrics;

    @JsonProperty("cases")
    private List<HandoverCaseTableRow> cases;

    public ExecutorHandoverDashboardResponse() {}

    public ExecutorHandoverDashboardResponse(DashboardMetrics metrics, List<HandoverCaseTableRow> cases) {
        this.metrics = metrics;
        this.cases = cases;
    }

    public DashboardMetrics getMetrics() {
        return metrics;
    }

    public void setMetrics(DashboardMetrics metrics) {
        this.metrics = metrics;
    }

    public List<HandoverCaseTableRow> getCases() {
        return cases;
    }

    public void setCases(List<HandoverCaseTableRow> cases) {
        this.cases = cases;
    }

    public static class DashboardMetrics {
        @JsonProperty("total_handover_cases")
        private int totalHandoverCases;

        @JsonProperty("pending_approval")
        private int pendingApproval;

        @JsonProperty("in_progress")
        private int inProgress;

        @JsonProperty("completed_releases")
        private int completedReleases;

        public DashboardMetrics() {}

        public DashboardMetrics(int totalHandoverCases, int pendingApproval, int inProgress, int completedReleases) {
            this.totalHandoverCases = totalHandoverCases;
            this.pendingApproval = pendingApproval;
            this.inProgress = inProgress;
            this.completedReleases = completedReleases;
        }

        public int getTotalHandoverCases() {
            return totalHandoverCases;
        }

        public void setTotalHandoverCases(int totalHandoverCases) {
            this.totalHandoverCases = totalHandoverCases;
        }

        public int getPendingApproval() {
            return pendingApproval;
        }

        public void setPendingApproval(int pendingApproval) {
            this.pendingApproval = pendingApproval;
        }

        public int getInProgress() {
            return inProgress;
        }

        public void setInProgress(int inProgress) {
            this.inProgress = inProgress;
        }

        public int getCompletedReleases() {
            return completedReleases;
        }

        public void setCompletedReleases(int completedReleases) {
            this.completedReleases = completedReleases;
        }
    }

    public static class HandoverCaseTableRow {
        @JsonProperty("case_id")
        private String caseId;

        @JsonProperty("case_number")
        private String caseNumber;

        @JsonProperty("vault_owner")
        private String vaultOwner;

        @JsonProperty("beneficiary")
        private String beneficiary;

        @JsonProperty("legal_verification_status")
        private String legalVerificationStatus;

        @JsonProperty("handover_status")
        private String handoverStatus;

        @JsonProperty("updated_date")
        private String updatedDate;

        @JsonProperty("action")
        private String action;

        public HandoverCaseTableRow() {}

        public HandoverCaseTableRow(String caseId, String caseNumber, String vaultOwner, String beneficiary,
                                   String legalVerificationStatus, String handoverStatus, String updatedDate, String action) {
            this.caseId = caseId;
            this.caseNumber = caseNumber;
            this.vaultOwner = vaultOwner;
            this.beneficiary = beneficiary;
            this.legalVerificationStatus = legalVerificationStatus;
            this.handoverStatus = handoverStatus;
            this.updatedDate = updatedDate;
            this.action = action;
        }

        public String getCaseId() {
            return caseId;
        }

        public void setCaseId(String caseId) {
            this.caseId = caseId;
        }

        public String getCaseNumber() {
            return caseNumber;
        }

        public void setCaseNumber(String caseNumber) {
            this.caseNumber = caseNumber;
        }

        public String getVaultOwner() {
            return vaultOwner;
        }

        public void setVaultOwner(String vaultOwner) {
            this.vaultOwner = vaultOwner;
        }

        public String getBeneficiary() {
            return beneficiary;
        }

        public void setBeneficiary(String beneficiary) {
            this.beneficiary = beneficiary;
        }

        public String getLegalVerificationStatus() {
            return legalVerificationStatus;
        }

        public void setLegalVerificationStatus(String legalVerificationStatus) {
            this.legalVerificationStatus = legalVerificationStatus;
        }

        public String getHandoverStatus() {
            return handoverStatus;
        }

        public void setHandoverStatus(String handoverStatus) {
            this.handoverStatus = handoverStatus;
        }

        public String getUpdatedDate() {
            return updatedDate;
        }

        public void setUpdatedDate(String updatedDate) {
            this.updatedDate = updatedDate;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }
    }
}
