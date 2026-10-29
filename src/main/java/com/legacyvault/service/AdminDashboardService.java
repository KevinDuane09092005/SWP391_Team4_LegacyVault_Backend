package com.legacyvault.service;

import com.legacyvault.dto.AdminDashboardResponse;
import com.legacyvault.entity.AccessRequest;
import com.legacyvault.entity.User;
import com.legacyvault.exception.AdminApiException;
import com.legacyvault.repository.AccessRequestRepository;
import com.legacyvault.repository.AuditLogRepository;
import com.legacyvault.repository.InAppNotificationRepository;
import com.legacyvault.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class AdminDashboardService {

    private static final Logger logger = LoggerFactory.getLogger(AdminDashboardService.class);

    private final UserRepository userRepository;
    private final AccessRequestRepository accessRequestRepository;
    private final InAppNotificationRepository notificationRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditLogService auditLogService;

    public AdminDashboardService(UserRepository userRepository,
                                AccessRequestRepository accessRequestRepository,
                                InAppNotificationRepository notificationRepository,
                                AuditLogRepository auditLogRepository,
                                AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.accessRequestRepository = accessRequestRepository;
        this.notificationRepository = notificationRepository;
        this.auditLogRepository = auditLogRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse getAdminDashboard() {
        long userCount = userRepository.count();
        long pendingReqCount = accessRequestRepository.countByStatus("PENDING");
        long notifCount = notificationRepository.count();
        long auditCount = auditLogRepository.count();

        AdminDashboardResponse.KpiMetrics kpi = new AdminDashboardResponse.KpiMetrics(
                Math.toIntExact(userCount), null,
                Math.toIntExact(pendingReqCount), null,
                Math.toIntExact(notifCount), null,
                Math.toIntExact(auditCount), null);

        List<AdminDashboardResponse.UserPreviewItem> usersPreview = new ArrayList<>();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

        List<User> users = userRepository.findAll();
        for (User u : users.subList(0, Math.min(users.size(), 4))) {
            String created = u.getCreatedAt() != null ? u.getCreatedAt().format(dtf) : "—";
            usersPreview.add(new AdminDashboardResponse.UserPreviewItem(
                    u.getFullName(),
                    toApiRole(u.getRole()),
                    effectiveStatus(u),
                    created));
        }

        List<AdminDashboardResponse.AccessRequestQueueItem> requestsQueue = new ArrayList<>();
        List<AccessRequest> pendingRequests = accessRequestRepository.findByStatusOrderByCreatedAtDesc("PENDING");
        for (AccessRequest req : pendingRequests) {
            requestsQueue.add(new AdminDashboardResponse.AccessRequestQueueItem(
                    req.getRequestId(), req.getRequesterName(), req.getRequestedRole() + ", " + req.getPurpose()));
        }

        Map<String, Long> roleCounts = new HashMap<>();
        for (User user : users) {
            roleCounts.merge(toApiRole(user.getRole()), 1L, Long::sum);
        }
        List<AdminDashboardResponse.RoleSummaryItem> rolesSummary = List.of(
                new AdminDashboardResponse.RoleSummaryItem("Admin", "Platform administration", roleCounts.getOrDefault("ADMIN", 0L).intValue()),
                new AdminDashboardResponse.RoleSummaryItem("Owner", "Vault asset management and assignments", roleCounts.getOrDefault("VAULT_OWNER", 0L).intValue()),
                new AdminDashboardResponse.RoleSummaryItem("Executor", "Handover management", roleCounts.getOrDefault("EXECUTOR", 0L).intValue()),
                new AdminDashboardResponse.RoleSummaryItem("Beneficiary", "Receive assigned legacy assets", roleCounts.getOrDefault("BENEFICIARY", 0L).intValue()),
                new AdminDashboardResponse.RoleSummaryItem("Verifier", "Review legal verification cases", roleCounts.getOrDefault("LEGAL_VERIFIER", 0L).intValue()));

        return new AdminDashboardResponse(kpi, usersPreview, requestsQueue, rolesSummary, List.of());
    }

    @Transactional
    public void approveAccessRequest(String requestId, String adminUserId) {
        AccessRequest req = getPendingRequest(requestId);
        req.setStatus("APPROVED");
        req.setReviewedBy(adminUserId);
        req.setReviewedAt(LocalDateTime.now());
        accessRequestRepository.save(req);
        recordRequestDecision(adminUserId, req, "ACCESS_REQUEST_APPROVED");
        logger.info("Admin {} approved access request {}", adminUserId, requestId);
    }

    @Transactional
    public void rejectAccessRequest(String requestId, String adminUserId) {
        AccessRequest req = getPendingRequest(requestId);
        req.setStatus("REJECTED");
        req.setReviewedBy(adminUserId);
        req.setReviewedAt(LocalDateTime.now());
        accessRequestRepository.save(req);
        recordRequestDecision(adminUserId, req, "ACCESS_REQUEST_REJECTED");
        logger.info("Admin {} rejected access request {}", adminUserId, requestId);
    }

    private AccessRequest getPendingRequest(String requestId) {
        AccessRequest request = accessRequestRepository.findById(requestId)
                .orElseThrow(() -> AdminApiException.notFound("Access request was not found."));
        if (!"PENDING".equalsIgnoreCase(request.getStatus())) {
            throw AdminApiException.conflict("This access request has already been reviewed.");
        }
        return request;
    }

    private void recordRequestDecision(String adminUserId, AccessRequest request, String action) {
        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> AdminApiException.notFound("Administrator account was not found."));
        auditLogService.recordLog(admin.getId(), admin.getFullName(), toApiRole(admin.getRole()), action,
                "Access request " + request.getRequestId(), "SUCCESS", null,
                "Request for " + request.getRequestedRole() + " reviewed as " + request.getStatus());
    }

    private String effectiveStatus(User user) {
        String status = user.getAccountStatus();
        return status == null || status.isBlank() ? "ACTIVE" : status.toUpperCase(Locale.ROOT);
    }

    private String toApiRole(String role) {
        if (role == null || role.isBlank()) return "VAULT_OWNER";
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        return normalized.equals("DIGITAL_EXECUTOR") ? "EXECUTOR" : normalized;
    }
}
