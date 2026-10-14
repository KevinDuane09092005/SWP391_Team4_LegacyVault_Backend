package com.legacyvault.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class DmsConfigRequest {

    @NotNull(message = "Check-in interval is required")
    @Min(value = 1, message = "Check-in interval must be at least 1 day")
    private Integer checkInIntervalDays;

    @NotNull(message = "Grace period is required")
    @Min(value = 1, message = "Grace period must be at least 1 day")
    private Integer gracePeriodDays;

    public DmsConfigRequest() {
    }

    public DmsConfigRequest(Integer checkInIntervalDays, Integer gracePeriodDays) {
        this.checkInIntervalDays = checkInIntervalDays;
        this.gracePeriodDays = gracePeriodDays;
    }

    public Integer getCheckInIntervalDays() {
        return checkInIntervalDays;
    }

    public void setCheckInIntervalDays(Integer checkInIntervalDays) {
        this.checkInIntervalDays = checkInIntervalDays;
    }

    public Integer getGracePeriodDays() {
        return gracePeriodDays;
    }

    public void setGracePeriodDays(Integer gracePeriodDays) {
        this.gracePeriodDays = gracePeriodDays;
    }
}
