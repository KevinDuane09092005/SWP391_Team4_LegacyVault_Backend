package com.legacyvault.service;

import com.legacyvault.dto.AdminUserListResponse;
import com.legacyvault.dto.AdminUserResponse;
import com.legacyvault.entity.User;
import com.legacyvault.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Set;

@Service
public class AdminUserManagementService {

    private static final Set<String> ALLOWED_ROLES = Set.of(
            "ADMIN", "VAULT_OWNER", "EXECUTOR", "BENEFICIARY", "LEGAL_VERIFIER");
    private static final Set<String> ALLOWED_STATUSES = Set.of("ACTIVE", "SUSPENDED");
    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    public AdminUserManagementService(UserRepository userRepository,
                                      AuditLogService auditLogService,
                                      NotificationService notificationService) {
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public AdminUserListResponse listUsers(String search, String role, String status, int page, int size) {
        validatePage(page, size);
        String roleFilter = role == null || role.isBlank() || "ALL".equalsIgnoreCase(role)
                ? null : normalizeRole(role);
        if (roleFilter != null && !ALLOWED_ROLES.contains(roleFilter)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown role filter");
        }
        String statusFilter = status == null || status.isBlank() ? "ALL" : status.trim().toUpperCase(Locale.ROOT);
        if (!"ALL".equals(statusFilter) && !ALLOWED_STATUSES.contains(statusFilter)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown status filter");
        }

        Page<User> result = userRepository.searchAdminUsers(
                search == null || search.isBlank() ? null : search.trim(),
                roleFilter, statusFilter,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return new AdminUserListResponse(
                result.getContent().stream().map(this::toResponse).toList(),
                new AdminUserListResponse.Pagination(page, size, result.getTotalElements(), result.getTotalPages()));
    }

    @Transactional
    public AdminUserResponse updateRole(String adminId, String targetUserId, String requestedRole) {
        String role = normalizeRole(requestedRole);
        if (!ALLOWED_ROLES.contains(role)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown role");
        }
        rejectSelfChange(adminId, targetUserId);
        User target = findUser(targetUserId);
        String previousRole = normalizeRole(target.getRole());
        if (previousRole.equals(role)) return toResponse(target);

        if ("ADMIN".equals(previousRole) && !"ADMIN".equals(role)
                && userRepository.countByRoleIgnoreCase("ADMIN") <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "The last administrator cannot be demoted");
        }

        target.setRole(role);
        userRepository.save(target);
        recordAdminChange(adminId, "ADMIN_USER_ROLE_CHANGED", target,
                "from=" + previousRole + ";to=" + role);
        notificationService.createNotification(target.getId(), "Account role updated",
                "Your LegacyVault account role was changed to " + role + ".",
                "ACCOUNT_ROLE_CHANGED", "Action required", null);
        return toResponse(target);
    }

    @Transactional
    public AdminUserResponse updateStatus(String adminId, String targetUserId, String requestedStatus) {
        String status = requestedStatus == null ? "" : requestedStatus.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_STATUSES.contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown status");
        }
        rejectSelfChange(adminId, targetUserId);
        User target = findUser(targetUserId);
        String previousStatus = Boolean.FALSE.equals(target.getIsActive()) ? "SUSPENDED" : "ACTIVE";
        if (previousStatus.equals(status)) return toResponse(target);

        target.setIsActive("ACTIVE".equals(status));
        userRepository.save(target);
        recordAdminChange(adminId, "ADMIN_USER_STATUS_CHANGED", target,
                "from=" + previousStatus + ";to=" + status);
        notificationService.createNotification(target.getId(), "Account status updated",
                "Your LegacyVault account status is now " + status + ".",
                "ACCOUNT_STATUS_CHANGED", "Action required", null);
        return toResponse(target);
    }

    private User findUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User was not found"));
    }

    private void rejectSelfChange(String adminId, String targetUserId) {
        if (adminId != null && adminId.equals(targetUserId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Administrators cannot change their own role or status");
        }
    }

    private void recordAdminChange(String adminId, String action, User target, String details) {
        User admin = userRepository.findById(adminId).orElse(null);
        auditLogService.recordLog(adminId,
                admin == null ? null : admin.getFullName(),
                admin == null || admin.getRole() == null ? "ADMIN" : AdminDashboardService.normalizeRole(admin.getRole()),
                action, target.getId(), "SUCCESS", null,
                details + ";target_email=" + target.getEmail());
    }

    private AdminUserResponse toResponse(User user) {
        String status = Boolean.FALSE.equals(user.getIsActive()) ? "SUSPENDED" : "ACTIVE";
        return new AdminUserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getEmail(),
                user.getRole() == null ? "" : AdminDashboardService.normalizeRole(user.getRole()),
                status, user.getCreatedAt());
    }

    private String normalizeRole(String role) {
        return role == null ? "" : AdminDashboardService.normalizeRole(role);
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Page must be non-negative and size must be between 1 and " + MAX_PAGE_SIZE);
        }
    }
}
