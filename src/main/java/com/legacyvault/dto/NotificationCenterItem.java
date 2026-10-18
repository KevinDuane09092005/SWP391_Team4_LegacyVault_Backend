package com.legacyvault.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class NotificationCenterItem {

    @JsonProperty("notification_id")
    private String notificationId;

    @JsonProperty("title")
    private String title;

    @JsonProperty("subtext")
    private String subtext;

    @JsonProperty("timestamp_formatted")
    private String timestampFormatted;

    @JsonProperty("badge_label")
    private String badgeLabel;

    @JsonProperty("is_read")
    private boolean isRead;

    public NotificationCenterItem() {}

    public NotificationCenterItem(String notificationId, String title, String subtext,
                                  String timestampFormatted, String badgeLabel, boolean isRead) {
        this.notificationId = notificationId;
        this.title = title;
        this.subtext = subtext;
        this.timestampFormatted = timestampFormatted;
        this.badgeLabel = badgeLabel;
        this.isRead = isRead;
    }

    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtext() {
        return subtext;
    }

    public void setSubtext(String subtext) {
        this.subtext = subtext;
    }

    public String getTimestampFormatted() {
        return timestampFormatted;
    }

    public void setTimestampFormatted(String timestampFormatted) {
        this.timestampFormatted = timestampFormatted;
    }

    public String getBadgeLabel() {
        return badgeLabel;
    }

    public void setBadgeLabel(String badgeLabel) {
        this.badgeLabel = badgeLabel;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }
}
