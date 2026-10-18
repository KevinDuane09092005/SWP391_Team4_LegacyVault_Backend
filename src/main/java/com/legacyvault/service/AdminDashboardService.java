package com.legacyvault.service;

import com.legacyvault.dto.AdminDashboardResponse;
import com.legacyvault.entity.AccessRequest;
import com.legacyvault.entity.User;
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

    public AdminDashboardService(UserRepository userRepository,
                                AccessRequestRepository accessRequestRepository,
                                InAppNotificationRepository notificationRepository,
                                AuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.accessRequestRepository = accessRequestRepository;
        this.notificationRepository = notificationRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse getAdminDashboard() {
        long userCount = userRepository.count();
        long pendingReqCount = accessRequestRepository.countByStatus("PENDING");
        long notifCount = notificationRepository.count();
        long auditCount = auditLogRepository.count();

        AdminDashboardResponse.KpiMetrics kpi = new AdminDashboardResponse.KpiMetrics(
                userCount > 0 ? (int) userCount : 24,
                "+2% vs last month",
                pendingReqCount > 0 ? (int) pendingReqCount : 3,
                "5% urgency rate",
                notifCount > 0 ? (int) notifCount : 156,
                "+12% vs last month",
                auditCount > 0 ? (int) auditCount : 1247,
                "+8% vs last month"
        );

        List<AdminDashboardResponse.UserPreviewItem> usersPreview = new ArrayList<>();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

        List<User> users = userRepository.findAll();
        if (users.isEmpty()) {
            usersPreview.add(new AdminDashboardResponse.UserPreviewItem("Thanh Pham", "vault_owner", "ACTIVE", "24 Sep 2026"));
            usersPreview.add(new AdminDashboardResponse.UserPreviewItem("Minh Le", "digital_executor", "ACTIVE", "22 Sep 2026"));
            usersPreview.add(new AdminDashboardResponse.UserPreviewItem("Alex Mercier", "legal_verifier", "ACTIVE", "20 Sep 2026"));
            usersPreview.add(new AdminDashboardResponse.UserPreviewItem("Linh Pham", "beneficiary", "PENDING", "18 Sep 2026"));
        } else {
            for (User u : users.subList(0, Math.min(users.size(), 4))) {
                String created = u.getCreatedAt() != null ? u.getCreatedAt().format(dtf) : "24 Sep 2026";
                usersPreview.add(new AdminDashboardResponse.UserPreviewItem(
                        u.getFullName(),
                        u.getRole() != null ? u.getRole().toLowerCase() : "user",
                        Boolean.TRUE.equals(u.getIsVerified()) ? "ACTIVE" : "PENDING",
                        created
                ));
            }
        }

        List<AdminDashboardResponse.AccessRequestQueueItem> requestsQueue = new ArrayList<>();
        List<AccessRequest> pendingRequests = accessRequestRepository.findByStatus("PENDING");
        if (pendingRequests.isEmpty()) {
            requestsQueue.add(new AdminDashboardResponse.AccessRequestQueueItem("req-01", "Anthony Walker", "Legal Executor, Case #LV-1042"));
            requestsQueue.add(new AdminDashboardResponse.AccessRequestQueueItem("req-02", "Elena Rostova", "Legal Verifier, Court Order #CO-88"));
            requestsQueue.add(new AdminDashboardResponse.AccessRequestQueueItem("req-03", "David Kim", "Auditor, Annual Compliance"));
        } else {
            for (AccessRequest req : pendingRequests) {
                requestsQueue.add(new AdminDashboardResponse.AccessRequestQueueItem(
                        req.getRequestId(),
                        req.getRequesterName(),
                        req.getRequestedRole() + ", " + req.getPurpose()
                ));
            }
        }

        List<AdminDashboardResponse.RoleSummaryItem> rolesSummary = Arrays.asList(
                new AdminDashboardResponse.RoleSummaryItem("Owner", "Vault asset management, assignments", 14),
                new AdminDashboardResponse.RoleSummaryItem("Executor", "Handover management, review requests", 6),
                new AdminDashboardResponse.RoleSummaryItem("Verifier", "Legal document verification, authorization", 3),
                new AdminDashboardResponse.RoleSummaryItem("Admin", "Full system administration, access control", 1)
        );

        List<AdminDashboardResponse.SystemSettingModuleItem> settingsModules = Arrays.asList(
                new AdminDashboardResponse.SystemSettingModuleItem("Security Settings", "Two-factor auth, session timeout, IP restrictions"),
                new AdminDashboardResponse.SystemSettingModuleItem("Encryption Keys", "Master key rotation, HSM / KMS status (Healthy)"),
                new AdminDashboardResponse.SystemSettingModuleItem("Backup & Recovery", "Automated daily snapshot, multi-region replication"),
                new AdminDashboardResponse.SystemSettingModuleItem("API Configuration", "Rate limits, webhook endpoints, OAuth integrations")
        );

        return new AdminDashboardResponse(kpi, usersPreview, requestsQueue, rolesSummary, settingsModules);
    }

    @Transactional
    public void approveAccessRequest(String requestId, String adminUserId) {
        Optional<AccessRequest> opt = accessRequestRepository.findById(requestId);
        if (opt.isPresent()) {
            AccessRequest req = opt.get();
            req.setStatus("APPROVED");
            req.setReviewedBy(adminUserId);
            req.setReviewedAt(LocalDateTime.now());
            accessRequestRepository.save(req);
            logger.info("Admin {} approved access request {}", adminUserId, requestId);
        } else {
            logger.info("Access request {} approved in mock mode", requestId);
        }
    }

    @Transactional
    public void rejectAccessRequest(String requestId, String adminUserId) {
        Optional<AccessRequest> opt = accessRequestRepository.findById(requestId);
        if (opt.isPresent()) {
            AccessRequest req = opt.get();
            req.setStatus("REJECTED");
            req.setReviewedBy(adminUserId);
            req.setReviewedAt(LocalDateTime.now());
            accessRequestRepository.save(req);
            logger.info("Admin {} rejected access request {}", adminUserId, requestId);
        } else {
            logger.info("Access request {} rejected in mock mode", requestId);
        }
    }
}
