package com.legacyvault.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AuditLogRow {

    @JsonProperty("time_formatted")
    private String timeFormatted;

    @JsonProperty("user")
    private String user;

    @JsonProperty("action")
    private String action;

    @JsonProperty("target")
    private String target;

    @JsonProperty("status")
    private String status;

    @JsonProperty("ip_address")
    private String ipAddress;

    public AuditLogRow() {}

    public AuditLogRow(String timeFormatted, String user, String action, String target, String status, String ipAddress) {
        this.timeFormatted = timeFormatted;
        this.user = user;
        this.action = action;
        this.target = target;
        this.status = status;
        this.ipAddress = ipAddress;
    }

    public String getTimeFormatted() {
        return timeFormatted;
    }

    public void setTimeFormatted(String timeFormatted) {
        this.timeFormatted = timeFormatted;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
}
