package com.legacyvault.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StartReleaseResponse {

    @JsonProperty("case_number")
    private String caseNumber;

    @JsonProperty("handover_status")
    private String handoverStatus;

    @JsonProperty("access_code")
    private String accessCode;

    @JsonProperty("notification_sent_to")
    private String notificationSentTo;

    public StartReleaseResponse() {}

    public StartReleaseResponse(String caseNumber, String handoverStatus, String accessCode, String notificationSentTo) {
        this.caseNumber = caseNumber;
        this.handoverStatus = handoverStatus;
        this.accessCode = accessCode;
        this.notificationSentTo = notificationSentTo;
    }

    public String getCaseNumber() {
        return caseNumber;
    }

    public void setCaseNumber(String caseNumber) {
        this.caseNumber = caseNumber;
    }

    public String getHandoverStatus() {
        return handoverStatus;
    }

    public void setHandoverStatus(String handoverStatus) {
        this.handoverStatus = handoverStatus;
    }

    public String getAccessCode() {
        return accessCode;
    }

    public void setAccessCode(String accessCode) {
        this.accessCode = accessCode;
    }

    public String getNotificationSentTo() {
        return notificationSentTo;
    }

    public void setNotificationSentTo(String notificationSentTo) {
        this.notificationSentTo = notificationSentTo;
    }
}
