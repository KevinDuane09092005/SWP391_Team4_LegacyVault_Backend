package com.legacyvault.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;

public class DmsConfigRequest {

    @Min(value = 1, message = "Check-in interval must be at least 1 second")
    @JsonProperty("checkInIntervalSeconds")
    private Integer checkInIntervalSeconds;

    @Min(value = 1, message = "Grace period must be at least 1 second")
    @JsonProperty("gracePeriodSeconds")
    private Integer gracePeriodSeconds;

    @JsonProperty("checkInIntervalDays")
    private Integer checkInIntervalDays;

    @JsonProperty("gracePeriodDays")
    private Integer gracePeriodDays;

    public DmsConfigRequest() {
    }

    public DmsConfigRequest(Integer checkInIntervalSeconds, Integer gracePeriodSeconds) {
        this.checkInIntervalSeconds = checkInIntervalSeconds;
        this.gracePeriodSeconds = gracePeriodSeconds;
        this.checkInIntervalDays = checkInIntervalSeconds;
        this.gracePeriodDays = gracePeriodSeconds;
    }

    public Integer getCheckInIntervalSeconds() {
        return checkInIntervalSeconds != null ? checkInIntervalSeconds : checkInIntervalDays;
    }

    public void setCheckInIntervalSeconds(Integer checkInIntervalSeconds) {
        this.checkInIntervalSeconds = checkInIntervalSeconds;
    }

    public Integer getGracePeriodSeconds() {
        return gracePeriodSeconds != null ? gracePeriodSeconds : gracePeriodDays;
    }

    public void setGracePeriodSeconds(Integer gracePeriodSeconds) {
        this.gracePeriodSeconds = gracePeriodSeconds;
    }

    public Integer getCheckInIntervalDays() {
        return getCheckInIntervalSeconds();
    }

    public void setCheckInIntervalDays(Integer checkInIntervalDays) {
        this.checkInIntervalDays = checkInIntervalDays;
        if (this.checkInIntervalSeconds == null) {
            this.checkInIntervalSeconds = checkInIntervalDays;
        }
    }

    public Integer getGracePeriodDays() {
        return getGracePeriodSeconds();
    }

    public void setGracePeriodDays(Integer gracePeriodDays) {
        this.gracePeriodDays = gracePeriodDays;
        if (this.gracePeriodSeconds == null) {
            this.gracePeriodSeconds = gracePeriodDays;
        }
    }
}
