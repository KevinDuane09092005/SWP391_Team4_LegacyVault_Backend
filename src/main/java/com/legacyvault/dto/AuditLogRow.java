package com.legacyvault.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class AuditLogRow {

    @JsonProperty("log_id")
    private String logId;

    @JsonProperty("timestamp_formatted")
    private String timestampFormatted;

    @JsonProperty("time_formatted")
    private String timeFormatted;

    @JsonIgnore
    private String user;

    @JsonProperty("role")
    private String role;

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
        this(null, timeFormatted, timeFormatted, user, "SYSTEM", action, target, status, ipAddress);
    }

    public AuditLogRow(String logId, String timestampFormatted, String timeFormatted, String user, String role,
                       String action, String target, String status, String ipAddress) {
        this.logId = logId;
        this.timestampFormatted = timestampFormatted;
        this.timeFormatted = timeFormatted;
        this.user = user;
        this.role = role;
        this.action = action;
        this.target = target;
        this.status = status;
        this.ipAddress = ipAddress;
    }

    public String getLogId() {
        return logId;
    }

    public void setLogId(String logId) {
        this.logId = logId;
    }

    public String getTimestampFormatted() {
        return timestampFormatted;
    }

    public void setTimestampFormatted(String timestampFormatted) {
        this.timestampFormatted = timestampFormatted;
    }

    public String getTimeFormatted() {
        return timeFormatted;
    }

    public void setTimeFormatted(String timeFormatted) {
        this.timeFormatted = timeFormatted;
    }

    @JsonIgnore
    public String getUser() {
        return user;
    }

    @JsonProperty("user")
    public Map<String, String> getUserObject() {
        return Map.of(
                "name", user != null ? user : "System User",
                "role", role != null ? role : "SYSTEM"
        );
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
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
