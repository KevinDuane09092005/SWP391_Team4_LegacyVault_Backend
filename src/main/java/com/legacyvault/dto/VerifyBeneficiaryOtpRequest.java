package com.legacyvault.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public class VerifyBeneficiaryOtpRequest {

    @NotBlank(message = "OTP code is required")
    @JsonProperty("otp_code")
    @JsonAlias("otpCode")
    private String otpCode;

    @JsonProperty("case_ref")
    @JsonAlias({"caseRef", "case_id", "caseId"})
    private String caseRef;

    public VerifyBeneficiaryOtpRequest() {}

    public VerifyBeneficiaryOtpRequest(String otpCode) {
        this.otpCode = otpCode;
    }

    public VerifyBeneficiaryOtpRequest(String otpCode, String caseRef) {
        this.otpCode = otpCode;
        this.caseRef = caseRef;
    }

    public String getOtpCode() {
        return otpCode;
    }

    public void setOtpCode(String otpCode) {
        this.otpCode = otpCode;
    }

    public String getCaseRef() {
        return caseRef;
    }

    public void setCaseRef(String caseRef) {
        this.caseRef = caseRef;
    }
}
