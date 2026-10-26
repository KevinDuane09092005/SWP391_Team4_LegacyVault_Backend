package com.legacyvault.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class AdminDashboardResponse {

    @JsonProperty("kpi_metrics")
    private KpiMetrics kpiMetrics;

    @JsonProperty("user_management_preview")
    private List<UserPreviewItem> userManagementPreview;

    @JsonProperty("access_requests_queue")
    private List<AccessRequestQueueItem> accessRequestsQueue;

    @JsonProperty("roles_and_permissions_summary")
    private List<RoleSummaryItem> rolesAndPermissionsSummary;

    @JsonProperty("system_settings_modules")
    private List<SystemSettingModuleItem> systemSettingsModules;

    public AdminDashboardResponse() {}

    public AdminDashboardResponse(KpiMetrics kpiMetrics, List<UserPreviewItem> userManagementPreview,
                                  List<AccessRequestQueueItem> accessRequestsQueue,
                                  List<RoleSummaryItem> rolesAndPermissionsSummary,
                                  List<SystemSettingModuleItem> systemSettingsModules) {
        this.kpiMetrics = kpiMetrics;
        this.userManagementPreview = userManagementPreview;
        this.accessRequestsQueue = accessRequestsQueue;
        this.rolesAndPermissionsSummary = rolesAndPermissionsSummary;
        this.systemSettingsModules = systemSettingsModules;
    }

    public KpiMetrics getKpiMetrics() {
        return kpiMetrics;
    }

    public void setKpiMetrics(KpiMetrics kpiMetrics) {
        this.kpiMetrics = kpiMetrics;
    }

    public List<UserPreviewItem> getUserManagementPreview() {
        return userManagementPreview;
    }

    public void setUserManagementPreview(List<UserPreviewItem> userManagementPreview) {
        this.userManagementPreview = userManagementPreview;
    }

    public List<AccessRequestQueueItem> getAccessRequestsQueue() {
        return accessRequestsQueue;
    }

    public void setAccessRequestsQueue(List<AccessRequestQueueItem> accessRequestsQueue) {
        this.accessRequestsQueue = accessRequestsQueue;
    }

    public List<RoleSummaryItem> getRolesAndPermissionsSummary() {
        return rolesAndPermissionsSummary;
    }

    public void setRolesAndPermissionsSummary(List<RoleSummaryItem> rolesAndPermissionsSummary) {
        this.rolesAndPermissionsSummary = rolesAndPermissionsSummary;
    }

    public List<SystemSettingModuleItem> getSystemSettingsModules() {
        return systemSettingsModules;
    }

    public void setSystemSettingsModules(List<SystemSettingModuleItem> systemSettingsModules) {
        this.systemSettingsModules = systemSettingsModules;
    }

    public static class KpiMetrics {
        @JsonProperty("total_users")
        private int totalUsers;

        @JsonProperty("total_users_change")
        private String totalUsersChange;

        @JsonProperty("pending_requests")
        private int pendingRequests;

        @JsonProperty("pending_requests_urgency")
        private String pendingRequestsUrgency;

        @JsonProperty("notifications_sent")
        private int notificationsSent;

        @JsonProperty("notifications_sent_change")
        private String notificationsSentChange;

        @JsonProperty("audit_events")
        private int auditEvents;

        @JsonProperty("audit_events_change")
        private String auditEventsChange;

        public KpiMetrics() {}

        public KpiMetrics(int totalUsers, String totalUsersChange, int pendingRequests, String pendingRequestsUrgency,
                          int notificationsSent, String notificationsSentChange, int auditEvents, String auditEventsChange) {
            this.totalUsers = totalUsers;
            this.totalUsersChange = totalUsersChange;
            this.pendingRequests = pendingRequests;
            this.pendingRequestsUrgency = pendingRequestsUrgency;
            this.notificationsSent = notificationsSent;
            this.notificationsSentChange = notificationsSentChange;
            this.auditEvents = auditEvents;
            this.auditEventsChange = auditEventsChange;
        }

        public int getTotalUsers() { return totalUsers; }
        public void setTotalUsers(int totalUsers) { this.totalUsers = totalUsers; }
        public String getTotalUsersChange() { return totalUsersChange; }
        public void setTotalUsersChange(String totalUsersChange) { this.totalUsersChange = totalUsersChange; }
        public int getPendingRequests() { return pendingRequests; }
        public void setPendingRequests(int pendingRequests) { this.pendingRequests = pendingRequests; }
        public String getPendingRequestsUrgency() { return pendingRequestsUrgency; }
        public void setPendingRequestsUrgency(String pendingRequestsUrgency) { this.pendingRequestsUrgency = pendingRequestsUrgency; }
        public int getNotificationsSent() { return notificationsSent; }
        public void setNotificationsSent(int notificationsSent) { this.notificationsSent = notificationsSent; }
        public String getNotificationsSentChange() { return notificationsSentChange; }
        public void setNotificationsSentChange(String notificationsSentChange) { this.notificationsSentChange = notificationsSentChange; }
        public int getAuditEvents() { return auditEvents; }
        public void setAuditEvents(int auditEvents) { this.auditEvents = auditEvents; }
        public String getAuditEventsChange() { return auditEventsChange; }
        public void setAuditEventsChange(String auditEventsChange) { this.auditEventsChange = auditEventsChange; }
    }

    public static class UserPreviewItem {
        @JsonProperty("user_id")
        private String userId;

        @JsonProperty("name")
        private String name;

        @JsonProperty("email")
        private String email;

        @JsonProperty("role")
        private String role;

        @JsonProperty("status")
        private String status;

        @JsonProperty("created_date_formatted")
        private String createdDateFormatted;

        public UserPreviewItem() {}

        public UserPreviewItem(String name, String role, String status, String createdDateFormatted) {
            this(null, name, null, role, status, createdDateFormatted);
        }

        public UserPreviewItem(String userId, String name, String email, String role, String status,
                               String createdDateFormatted) {
            this.userId = userId;
            this.name = name;
            this.email = email;
            this.role = role;
            this.status = status;
            this.createdDateFormatted = createdDateFormatted;
        }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getCreatedDateFormatted() { return createdDateFormatted; }
        public void setCreatedDateFormatted(String createdDateFormatted) { this.createdDateFormatted = createdDateFormatted; }
    }

    public static class AccessRequestQueueItem {
        @JsonProperty("request_id")
        private String requestId;

        @JsonProperty("requester_name")
        private String requesterName;

        @JsonProperty("requested_role_or_purpose")
        private String requestedRoleOrPurpose;

        public AccessRequestQueueItem() {}

        public AccessRequestQueueItem(String requestId, String requesterName, String requestedRoleOrPurpose) {
            this.requestId = requestId;
            this.requesterName = requesterName;
            this.requestedRoleOrPurpose = requestedRoleOrPurpose;
        }

        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }
        public String getRequesterName() { return requesterName; }
        public void setRequesterName(String requesterName) { this.requesterName = requesterName; }
        public String getRequestedRoleOrPurpose() { return requestedRoleOrPurpose; }
        public void setRequestedRoleOrPurpose(String requestedRoleOrPurpose) { this.requestedRoleOrPurpose = requestedRoleOrPurpose; }
    }

    public static class RoleSummaryItem {
        @JsonProperty("role_name")
        private String roleName;

        @JsonProperty("description")
        private String description;

        @JsonProperty("user_count")
        private int userCount;

        public RoleSummaryItem() {}

        public RoleSummaryItem(String roleName, String description, int userCount) {
            this.roleName = roleName;
            this.description = description;
            this.userCount = userCount;
        }

        public String getRoleName() { return roleName; }
        public void setRoleName(String roleName) { this.roleName = roleName; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public int getUserCount() { return userCount; }
        public void setUserCount(int userCount) { this.userCount = userCount; }
    }

    public static class SystemSettingModuleItem {
        @JsonProperty("module_name")
        private String moduleName;

        @JsonProperty("summary")
        private String summary;

        public SystemSettingModuleItem() {}

        public SystemSettingModuleItem(String moduleName, String summary) {
            this.moduleName = moduleName;
            this.summary = summary;
        }

        public String getModuleName() { return moduleName; }
        public void setModuleName(String moduleName) { this.moduleName = moduleName; }
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
    }
}
