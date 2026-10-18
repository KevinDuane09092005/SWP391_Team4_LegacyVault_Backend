package com.legacyvault.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public class ConfirmReceivedRequest {

    @NotNull(message = "accept_digital_estate_custody must be specified")
    @JsonProperty("accept_digital_estate_custody")
    private Boolean acceptDigitalEstateCustody;

    @JsonProperty("feedback_notes")
    private String feedbackNotes;

    public ConfirmReceivedRequest() {}

    public ConfirmReceivedRequest(Boolean acceptDigitalEstateCustody) {
        this.acceptDigitalEstateCustody = acceptDigitalEstateCustody;
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
