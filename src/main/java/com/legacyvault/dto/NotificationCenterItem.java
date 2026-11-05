package com.legacyvault.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public class NotificationCenterItem {

    @JsonProperty("notification_id")
    @JsonAlias("notificationId")
    private String notificationId;

    @JsonProperty("title")
    private String title;

    @JsonProperty("subtext")
    private String subtext;

    @JsonProperty("timestamp_formatted")
    @JsonAlias("timestampFormatted")
    private String timestampFormatted;

    @JsonProperty("badge_label")
    @JsonAlias("badgeLabel")
    private String badgeLabel;

    @JsonProperty("is_read")
    @JsonAlias("isRead")
    private boolean isRead;

    @JsonProperty("case_id")
    @JsonAlias("caseId")
    private String caseId;

    @JsonProperty("deep_link")
    @JsonAlias("deepLink")
    private String deepLink;

    public NotificationCenterItem() {}

    public NotificationCenterItem(String notificationId, String title, String subtext,
                                  String timestampFormatted, String badgeLabel, boolean isRead) {
        this(notificationId, title, subtext, timestampFormatted, badgeLabel, isRead, null, null);
    }

    public NotificationCenterItem(String notificationId, String title, String subtext,
                                  String timestampFormatted, String badgeLabel, boolean isRead,
                                  String caseId, String deepLink) {
        this.notificationId = notificationId;
        this.title = title;
        this.subtext = subtext;
        this.timestampFormatted = timestampFormatted;
        this.badgeLabel = badgeLabel;
        this.isRead = isRead;
        this.caseId = caseId;
        this.deepLink = deepLink;
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

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public String getDeepLink() {
        return deepLink;
    }

    public void setDeepLink(String deepLink) {
        this.deepLink = deepLink;
    }
}
