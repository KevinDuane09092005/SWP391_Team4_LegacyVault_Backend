package com.legacyvault.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class HandoverCompletedResponse {

    @JsonProperty("case_id")
    private String caseId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("title")
    private String title;

    @JsonProperty("subtitle")
    private String subtitle;

    @JsonProperty("estate_deposition_trans_receipt")
    private EstateDepositionTransReceipt estateDepositionTransReceipt;

    @JsonProperty("download_receipt_certificate_url")
    private String downloadReceiptCertificateUrl;

    public HandoverCompletedResponse() {}

    public HandoverCompletedResponse(String caseId, String status, String title, String subtitle,
                                   EstateDepositionTransReceipt estateDepositionTransReceipt,
                                   String downloadReceiptCertificateUrl) {
        this.caseId = caseId;
        this.status = status;
        this.title = title;
        this.subtitle = subtitle;
        this.estateDepositionTransReceipt = estateDepositionTransReceipt;
        this.downloadReceiptCertificateUrl = downloadReceiptCertificateUrl;
    }

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public EstateDepositionTransReceipt getEstateDepositionTransReceipt() {
        return estateDepositionTransReceipt;
    }

    public void setEstateDepositionTransReceipt(EstateDepositionTransReceipt estateDepositionTransReceipt) {
        this.estateDepositionTransReceipt = estateDepositionTransReceipt;
    }

    public String getDownloadReceiptCertificateUrl() {
        return downloadReceiptCertificateUrl;
    }

    public void setDownloadReceiptCertificateUrl(String downloadReceiptCertificateUrl) {
        this.downloadReceiptCertificateUrl = downloadReceiptCertificateUrl;
    }

    public static class EstateDepositionTransReceipt {
        @JsonProperty("deceased_owner")
        private String deceasedOwner;

        @JsonProperty("beneficiary_recipient")
        private String beneficiaryRecipient;

        @JsonProperty("executor_legal_rep")
        private String executorLegalRep;

        @JsonProperty("finalization_date")
        private String finalizationDate;

        public EstateDepositionTransReceipt() {}

        public EstateDepositionTransReceipt(String deceasedOwner, String beneficiaryRecipient, String executorLegalRep, String finalizationDate) {
            this.deceasedOwner = deceasedOwner;
            this.beneficiaryRecipient = beneficiaryRecipient;
            this.executorLegalRep = executorLegalRep;
            this.finalizationDate = finalizationDate;
        }

        public String getDeceasedOwner() { return deceasedOwner; }
        public void setDeceasedOwner(String deceasedOwner) { this.deceasedOwner = deceasedOwner; }
        public String getBeneficiaryRecipient() { return beneficiaryRecipient; }
        public void setBeneficiaryRecipient(String beneficiaryRecipient) { this.beneficiaryRecipient = beneficiaryRecipient; }
        public String getExecutorLegalRep() { return executorLegalRep; }
        public void setExecutorLegalRep(String executorLegalRep) { this.executorLegalRep = executorLegalRep; }
        public String getFinalizationDate() { return finalizationDate; }
        public void setFinalizationDate(String finalizationDate) { this.finalizationDate = finalizationDate; }
    }
}
