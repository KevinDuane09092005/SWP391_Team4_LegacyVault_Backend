package com.legacyvault.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VerifyBeneficiaryOtpResponse {

    @JsonProperty("otp_verified")
    private boolean otpVerified;

    @JsonProperty("access_token")
    private String accessToken;

    public VerifyBeneficiaryOtpResponse() {}

    public VerifyBeneficiaryOtpResponse(boolean otpVerified, String accessToken) {
        this.otpVerified = otpVerified;
        this.accessToken = accessToken;
    }

    public boolean isOtpVerified() {
        return otpVerified;
    }

    public void setOtpVerified(boolean otpVerified) {
        this.otpVerified = otpVerified;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
