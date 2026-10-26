package com.legacyvault.service;

import com.legacyvault.dto.AdminDashboardResponse;
import com.legacyvault.entity.AccessRequest;
import com.legacyvault.entity.User;
import com.legacyvault.repository.AccessRequestRepository;
import com.legacyvault.repository.AuditLogRepository;
import com.legacyvault.repository.InAppNotificationRepository;
import com.legacyvault.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminDashboardService {

    private static final int PREVIEW_LIMIT = 5;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    private static final List<RoleDefinition> ROLE_DEFINITIONS = List.of(
            new RoleDefinition("VAULT_OWNER", "Vault and asset management"),
            new RoleDefinition("EXECUTOR", "Handover management and review requests"),
            new RoleDefinition("BENEFICIARY", "Receive and access released assets"),
            new RoleDefinition("LEGAL_VERIFIER", "Review legal evidence"),
            new RoleDefinition("ADMIN", "System administration and access control")
    );

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
        long pendingRequestCount = accessRequestRepository.countByStatus("PENDING");
        long notificationCount = notificationRepository.count();
        long auditCount = auditLogRepository.count();

        AdminDashboardResponse.KpiMetrics metrics = new AdminDashboardResponse.KpiMetrics(
                toInt(userCount), "—",
                toInt(pendingRequestCount), "—",
                toInt(notificationCount), "—",
                toInt(auditCount), "—");

        List<User> users = userRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        List<AdminDashboardResponse.UserPreviewItem> userPreview = users.stream()
                .limit(PREVIEW_LIMIT)
                .map(this::toPreview)
                .toList();

        List<AccessRequest> pendingRequests = new ArrayList<>(accessRequestRepository.findByStatus("PENDING"));
        pendingRequests.sort(Comparator.comparing(
                AccessRequest::getCreatedAt,
                Comparator.nullsLast(Comparator.naturalOrder())));
        List<AdminDashboardResponse.AccessRequestQueueItem> requestQueue = pendingRequests.stream()
                .limit(PREVIEW_LIMIT)
                .map(request -> new AdminDashboardResponse.AccessRequestQueueItem(
                        request.getRequestId(), request.getRequesterName(),
                        request.getRequestedRole() + ", " + request.getPurpose()))
                .toList();

        Map<String, Long> countsByRole = users.stream()
                .filter(user -> user.getRole() != null)
                .collect(Collectors.groupingBy(user -> normalizeRole(user.getRole()), Collectors.counting()));
        List<AdminDashboardResponse.RoleSummaryItem> roles = ROLE_DEFINITIONS.stream()
                .map(role -> new AdminDashboardResponse.RoleSummaryItem(
                        role.name(), role.description(), toInt(countsByRole.getOrDefault(role.name(), 0L))))
                .toList();

        // Security, backup and integration settings are not backed by live configuration in this project.
        // Return no fabricated operational state until those systems have real implementations.
        return new AdminDashboardResponse(metrics, userPreview, requestQueue, roles, List.of());
    }

    @Transactional
    public void approveAccessRequest(String requestId, String adminUserId) {
        decideAccessRequest(requestId, adminUserId, "APPROVED");
    }

    @Transactional
    public void rejectAccessRequest(String requestId, String adminUserId) {
        decideAccessRequest(requestId, adminUserId, "REJECTED");
    }

    private void decideAccessRequest(String requestId, String adminUserId, String decision) {
        AccessRequest request = accessRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Access request was not found"));
        if (!"PENDING".equalsIgnoreCase(request.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only pending access requests can be reviewed");
        }

        request.setStatus(decision);
        request.setReviewedBy(adminUserId);
        request.setReviewedAt(LocalDateTime.now());
        accessRequestRepository.save(request);

        User admin = userRepository.findById(adminUserId).orElse(null);
        auditLogService.recordLog(
                adminUserId,
                admin == null ? null : admin.getFullName(),
                admin == null || admin.getRole() == null ? "ADMIN" : normalizeRole(admin.getRole()),
                "ACCESS_REQUEST_" + decision,
                requestId,
                "SUCCESS",
                null,
                "Reviewed request for role " + request.getRequestedRole());
    }

    private AdminDashboardResponse.UserPreviewItem toPreview(User user) {
        String created = user.getCreatedAt() == null ? "—" : user.getCreatedAt().format(DATE_FORMAT);
        String status = Boolean.FALSE.equals(user.getIsActive())
                ? "SUSPENDED"
                : Boolean.TRUE.equals(user.getIsVerified()) ? "ACTIVE" : "PENDING";
        return new AdminDashboardResponse.UserPreviewItem(
                user.getId(), user.getFullName(), user.getEmail(),
                user.getRole() == null ? "USER" : normalizeRole(user.getRole()), status, created);
    }

    static String normalizeRole(String role) {
        String normalized = role.trim().replace('-', '_').toUpperCase(Locale.ROOT);
        return "DIGITAL_EXECUTOR".equals(normalized) ? "EXECUTOR" : normalized;
    }

    private int toInt(long count) {
        return (int) Math.min(count, Integer.MAX_VALUE);
    }

    private record RoleDefinition(String name, String description) {}
}
