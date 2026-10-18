package com.legacyvault.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BeneficiaryLoginSuccessResponse {

    @JsonProperty("claim_session_token")
    private String claimSessionToken;

    @JsonProperty("beneficiary_name")
    private String beneficiaryName;

    @JsonProperty("masked_phone_number")
    private String maskedPhoneNumber;

    @JsonProperty("requires_otp")
    private boolean requiresOtp;

    public BeneficiaryLoginSuccessResponse() {}

    public BeneficiaryLoginSuccessResponse(String claimSessionToken, String beneficiaryName, String maskedPhoneNumber, boolean requiresOtp) {
        this.claimSessionToken = claimSessionToken;
        this.beneficiaryName = beneficiaryName;
        this.maskedPhoneNumber = maskedPhoneNumber;
        this.requiresOtp = requiresOtp;
    }

    public String getClaimSessionToken() {
        return claimSessionToken;
    }

    public void setClaimSessionToken(String claimSessionToken) {
        this.claimSessionToken = claimSessionToken;
    }

    public String getBeneficiaryName() {
        return beneficiaryName;
    }

    public void setBeneficiaryName(String beneficiaryName) {
        this.beneficiaryName = beneficiaryName;
    }

    public String getMaskedPhoneNumber() {
        return maskedPhoneNumber;
    }

    public void setMaskedPhoneNumber(String maskedPhoneNumber) {
        this.maskedPhoneNumber = maskedPhoneNumber;
    }

    public boolean isRequiresOtp() {
        return requiresOtp;
    }

    public void setRequiresOtp(boolean requiresOtp) {
        this.requiresOtp = requiresOtp;
    }
}
