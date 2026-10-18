package com.legacyvault.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class NotificationDetailResponse {

    @JsonProperty("notification_id")
    private String notificationId;

    @JsonProperty("title")
    private String title;

    @JsonProperty("badge_label")
    private String badgeLabel;

    @JsonProperty("action_required_details")
    private String actionRequiredDetails;

    @JsonProperty("event_metadata")
    private EventMetadata eventMetadata;

    @JsonProperty("related_activity_timeline")
    private List<TimelineItem> relatedActivityTimeline;

    public NotificationDetailResponse() {}

    public NotificationDetailResponse(String notificationId, String title, String badgeLabel,
                                      String actionRequiredDetails, EventMetadata eventMetadata,
                                      List<TimelineItem> relatedActivityTimeline) {
        this.notificationId = notificationId;
        this.title = title;
        this.badgeLabel = badgeLabel;
        this.actionRequiredDetails = actionRequiredDetails;
        this.eventMetadata = eventMetadata;
        this.relatedActivityTimeline = relatedActivityTimeline;
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

    public String getBadgeLabel() {
        return badgeLabel;
    }

    public void setBadgeLabel(String badgeLabel) {
        this.badgeLabel = badgeLabel;
    }

    public String getActionRequiredDetails() {
        return actionRequiredDetails;
    }

    public void setActionRequiredDetails(String actionRequiredDetails) {
        this.actionRequiredDetails = actionRequiredDetails;
    }

    public EventMetadata getEventMetadata() {
        return eventMetadata;
    }

    public void setEventMetadata(EventMetadata eventMetadata) {
        this.eventMetadata = eventMetadata;
    }

    public List<TimelineItem> getRelatedActivityTimeline() {
        return relatedActivityTimeline;
    }

    public void setRelatedActivityTimeline(List<TimelineItem> relatedActivityTimeline) {
        this.relatedActivityTimeline = relatedActivityTimeline;
    }

    public static class EventMetadata {
        @JsonProperty("case_id")
        private String caseId;

        @JsonProperty("related_user")
        private String relatedUser;

        @JsonProperty("event_time")
        private String eventTime;

        @JsonProperty("event_type")
        private String eventType;

        @JsonProperty("authority")
        private String authority;

        public EventMetadata() {}

        public EventMetadata(String caseId, String relatedUser, String eventTime, String eventType, String authority) {
            this.caseId = caseId;
            this.relatedUser = relatedUser;
            this.eventTime = eventTime;
            this.eventType = eventType;
            this.authority = authority;
        }

        public String getCaseId() { return caseId; }
        public void setCaseId(String caseId) { this.caseId = caseId; }
        public String getRelatedUser() { return relatedUser; }
        public void setRelatedUser(String relatedUser) { this.relatedUser = relatedUser; }
        public String getEventTime() { return eventTime; }
        public void setEventTime(String eventTime) { this.eventTime = eventTime; }
        public String getEventType() { return eventType; }
        public void setEventType(String eventType) { this.eventType = eventType; }
        public String getAuthority() { return authority; }
        public void setAuthority(String authority) { this.authority = authority; }
    }

    public static class TimelineItem {
        @JsonProperty("stage_title")
        private String stageTitle;

        @JsonProperty("description")
        private String description;

        @JsonProperty("timestamp_formatted")
        private String timestampFormatted;

        public TimelineItem() {}

        public TimelineItem(String stageTitle, String description, String timestampFormatted) {
            this.stageTitle = stageTitle;
            this.description = description;
            this.timestampFormatted = timestampFormatted;
        }

        public String getStageTitle() { return stageTitle; }
        public void setStageTitle(String stageTitle) { this.stageTitle = stageTitle; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getTimestampFormatted() { return timestampFormatted; }
        public void setTimestampFormatted(String timestampFormatted) { this.timestampFormatted = timestampFormatted; }
    }
}
