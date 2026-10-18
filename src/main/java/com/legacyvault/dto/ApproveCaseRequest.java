package com.legacyvault.dto;

public class ApproveCaseRequest {
    private String signatureNote;
    private String verifierPin;
    private String verifierSignaturePayload;

    public ApproveCaseRequest() {
    }

    public String getSignatureNote() {
        return signatureNote;
    }

    public void setSignatureNote(String signatureNote) {
        this.signatureNote = signatureNote;
    }

    public String getVerifierPin() {
        return verifierPin;
    }

    public void setVerifierPin(String verifierPin) {
        this.verifierPin = verifierPin;
    }

    public String getVerifierSignaturePayload() {
        return verifierSignaturePayload;
    }

    public void setVerifierSignaturePayload(String verifierSignaturePayload) {
        this.verifierSignaturePayload = verifierSignaturePayload;
    }
}
