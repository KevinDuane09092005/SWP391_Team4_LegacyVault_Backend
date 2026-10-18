package com.legacyvault.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public class VerifyBeneficiaryOtpRequest {

    @NotBlank(message = "OTP code is required")
    @JsonProperty("otp_code")
    private String otpCode;

    public VerifyBeneficiaryOtpRequest() {}

    public VerifyBeneficiaryOtpRequest(String otpCode) {
        this.otpCode = otpCode;
    }

    public String getOtpCode() {
        return otpCode;
    }

    public void setOtpCode(String otpCode) {
        this.otpCode = otpCode;
    }
}
