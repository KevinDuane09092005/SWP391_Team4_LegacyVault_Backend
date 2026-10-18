package com.legacyvault.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class BeneficiaryLoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @JsonProperty("email")
    private String email;

    @NotBlank(message = "Access code is required")
    @JsonProperty("access_code")
    private String accessCode;

    public BeneficiaryLoginRequest() {}

    public BeneficiaryLoginRequest(String email, String accessCode) {
        this.email = email;
        this.accessCode = accessCode;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAccessCode() {
        return accessCode;
    }

    public void setAccessCode(String accessCode) {
        this.accessCode = accessCode;
    }
}
