package com.legacyvault.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public class ConfirmReceivedRequest {

    @NotNull(message = "accept_digital_estate_custody must be specified")
    @JsonProperty("accept_digital_estate_custody")
    @JsonAlias("acceptDigitalEstateCustody")
    private Boolean acceptDigitalEstateCustody;

    @JsonProperty("feedback_notes")
    @JsonAlias({"feedbackNotes", "signatureNote", "signature_note"})
    private String feedbackNotes;

    public ConfirmReceivedRequest() {}

    public ConfirmReceivedRequest(Boolean acceptDigitalEstateCustody) {
        this.acceptDigitalEstateCustody = acceptDigitalEstateCustody;
    }

    public ConfirmReceivedRequest(Boolean acceptDigitalEstateCustody, String feedbackNotes) {
        this.acceptDigitalEstateCustody = acceptDigitalEstateCustody;
        this.feedbackNotes = feedbackNotes;
    }

    public Boolean getAcceptDigitalEstateCustody() {
        return acceptDigitalEstateCustody;
    }

    public void setAcceptDigitalEstateCustody(Boolean acceptDigitalEstateCustody) {
        this.acceptDigitalEstateCustody = acceptDigitalEstateCustody;
    }

    public String getFeedbackNotes() {
        return feedbackNotes;
    }

    public void setFeedbackNotes(String feedbackNotes) {
        this.feedbackNotes = feedbackNotes;
    }
}
