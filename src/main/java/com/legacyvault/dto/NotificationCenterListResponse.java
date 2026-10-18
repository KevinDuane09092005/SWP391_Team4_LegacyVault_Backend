package com.legacyvault.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class NotificationCenterListResponse {

    @JsonProperty("notifications")
    private List<NotificationCenterItem> notifications;

    @JsonProperty("unread_count")
    private int unreadCount;

    public NotificationCenterListResponse() {}

    public NotificationCenterListResponse(List<NotificationCenterItem> notifications, int unreadCount) {
        this.notifications = notifications;
        this.unreadCount = unreadCount;
    }

    public List<NotificationCenterItem> getNotifications() {
        return notifications;
    }

    public void setNotifications(List<NotificationCenterItem> notifications) {
        this.notifications = notifications;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }
}
