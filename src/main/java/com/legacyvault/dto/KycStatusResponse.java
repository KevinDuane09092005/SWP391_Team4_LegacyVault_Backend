package com.legacyvault.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class KycStatusResponse {

    @JsonProperty("government_photo_id_status")
    private String governmentPhotoIdStatus;

    @JsonProperty("biometric_facial_verification_status")
    private String biometricFacialVerificationStatus;

    public KycStatusResponse() {}

    public KycStatusResponse(String governmentPhotoIdStatus, String biometricFacialVerificationStatus) {
        this.governmentPhotoIdStatus = governmentPhotoIdStatus;
        this.biometricFacialVerificationStatus = biometricFacialVerificationStatus;
    }

    public String getGovernmentPhotoIdStatus() {
        return governmentPhotoIdStatus;
    }

    public void setGovernmentPhotoIdStatus(String governmentPhotoIdStatus) {
        this.governmentPhotoIdStatus = governmentPhotoIdStatus;
    }

    public String getBiometricFacialVerificationStatus() {
        return biometricFacialVerificationStatus;
    }

    public void setBiometricFacialVerificationStatus(String biometricFacialVerificationStatus) {
        this.biometricFacialVerificationStatus = biometricFacialVerificationStatus;
    }
}
