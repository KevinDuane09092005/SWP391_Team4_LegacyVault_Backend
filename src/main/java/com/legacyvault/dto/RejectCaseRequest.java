package com.legacyvault.dto;

public class RejectCaseRequest {
    private String reasonCode;
    private String reason;

    public RejectCaseRequest() {
    }

    public RejectCaseRequest(String reasonCode, String reason) {
        this.reasonCode = reasonCode;
        this.reason = reason;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
