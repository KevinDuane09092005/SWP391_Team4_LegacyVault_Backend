package com.legacyvault.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BeneficiaryPortalContextResponse {

    @JsonProperty("case_reference")
    private String caseReference;

    @JsonProperty("deceased_owner")
    private String deceasedOwner;

    @JsonProperty("authorized_beneficiary")
    private String authorizedBeneficiary;

    public BeneficiaryPortalContextResponse() {}

    public BeneficiaryPortalContextResponse(String caseReference, String deceasedOwner, String authorizedBeneficiary) {
        this.caseReference = caseReference;
        this.deceasedOwner = deceasedOwner;
        this.authorizedBeneficiary = authorizedBeneficiary;
    }

    public String getCaseReference() {
        return caseReference;
    }

    public void setCaseReference(String caseReference) {
        this.caseReference = caseReference;
    }

    public String getDeceasedOwner() {
        return deceasedOwner;
    }

    public void setDeceasedOwner(String deceasedOwner) {
        this.deceasedOwner = deceasedOwner;
    }

    public String getAuthorizedBeneficiary() {
        return authorizedBeneficiary;
    }

    public void setAuthorizedBeneficiary(String authorizedBeneficiary) {
        this.authorizedBeneficiary = authorizedBeneficiary;
    }
}
