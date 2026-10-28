package com.legacyvault.service;

import com.legacyvault.dto.AdminUserPageResponse;
import com.legacyvault.dto.AdminUserResponse;
import com.legacyvault.entity.User;
import com.legacyvault.exception.AdminApiException;
import com.legacyvault.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AdminUserService {

    private static final Map<String, String> API_TO_STORED_ROLE = Map.of(
            "ADMIN", "admin",
            "SYSTEM_ADMIN", "admin",
            "VAULT_OWNER", "vault_owner",
            "EXECUTOR", "digital_executor",
            "DIGITAL_EXECUTOR", "digital_executor",
            "BENEFICIARY", "beneficiary",
            "LEGAL_VERIFIER", "legal_verifier"
    );

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    public AdminUserService(UserRepository userRepository,
                            AuditLogService auditLogService,
                            NotificationService notificationService) {
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public AdminUserPageResponse listUsers(String search, String role, String status, int page, int size) {
        if (page < 0) {
            throw AdminApiException.badRequest("Page must be zero or greater.");
        }
        if (size < 1 || size > 100) {
            throw AdminApiException.badRequest("Page size must be between 1 and 100.");
        }

        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
        String normalizedRole = normalizeRoleFilter(role);
        String normalizedStatus = normalizeStatusFilter(status);
        Page<User> result = userRepository.searchAdminUsers(
                normalizedSearch,
                normalizedRole,
                normalizedStatus,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        List<AdminUserResponse> users = result.getContent().stream()
                .map(this::toResponse)
                .toList();
        return new AdminUserPageResponse(users, new AdminUserPageResponse.Pagination(
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages()));
    }

    @Transactional
    public AdminUserResponse updateStatus(String adminId, String userId, String status, String ipAddress) {
        String normalizedStatus = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (!normalizedStatus.equals("ACTIVE") && !normalizedStatus.equals("SUSPENDED")) {
            throw AdminApiException.badRequest("Status must be ACTIVE or SUSPENDED.");
        }
        User target = getTargetAndCheckSelfChange(adminId, userId);
        String previousStatus = effectiveStatus(target);
        if (previousStatus.equals(normalizedStatus)) {
            return toResponse(target);
        }

        target.setAccountStatus(normalizedStatus);
        User saved = userRepository.save(target);
        recordChange(adminId, saved, "USER_STATUS_CHANGED", previousStatus, normalizedStatus, ipAddress);
        notificationService.createNotification(
                saved.getId(),
                "Account status updated",
                "An administrator changed your account status to " + normalizedStatus + ".",
                "ACCOUNT_STATUS_CHANGED",
                normalizedStatus,
                null);
        return toResponse(saved);
    }

    @Transactional
    public AdminUserResponse updateRole(String adminId, String userId, String role, String ipAddress) {
        String normalizedRole = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
        String storedRole = API_TO_STORED_ROLE.get(normalizedRole);
        if (storedRole == null) {
            throw AdminApiException.badRequest("Role is not supported.");
        }
        User target = getTargetAndCheckSelfChange(adminId, userId);
        String previousRole = toApiRole(target.getRole());
        String nextApiRole = toApiRole(storedRole);
        if (previousRole.equals(nextApiRole)) {
            return toResponse(target);
        }

        target.setRole(storedRole);
        User saved = userRepository.save(target);
        recordChange(adminId, saved, "USER_ROLE_CHANGED", previousRole, nextApiRole, ipAddress);
        notificationService.createNotification(
                saved.getId(),
                "Account role updated",
                "An administrator changed your account role to " + nextApiRole + ".",
                "ACCOUNT_ROLE_CHANGED",
                "Updated",
                null);
        return toResponse(saved);
    }

    private User getTargetAndCheckSelfChange(String adminId, String userId) {
        if (adminId != null && adminId.equals(userId)) {
            throw AdminApiException.badRequest("You cannot change your own account.");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> AdminApiException.notFound("User was not found."));
    }

    private void recordChange(String adminId, User target, String action,
                              String previousValue, String nextValue, String ipAddress) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> AdminApiException.notFound("Administrator account was not found."));
        String detail = "Changed " + action + " from " + previousValue + " to " + nextValue;
        auditLogService.recordLog(
                admin.getId(), admin.getFullName(), toApiRole(admin.getRole()), action,
                "User " + target.getId(), "SUCCESS", ipAddress, detail);
    }

    private AdminUserResponse toResponse(User user) {
        return new AdminUserResponse(
                user.getId(), user.getId(), user.getFullName(), user.getFullName(),
                user.getEmail(), user.getEmail(), toApiRole(user.getRole()), effectiveStatus(user), user.getCreatedAt());
    }

    private String normalizeRoleFilter(String role) {
        if (role == null || role.isBlank() || role.equalsIgnoreCase("ALL")) {
            return null;
        }
        String storedRole = API_TO_STORED_ROLE.get(role.trim().toUpperCase(Locale.ROOT));
        if (storedRole == null) {
            throw AdminApiException.badRequest("Role filter is not supported.");
        }
        return storedRole;
    }

    private String normalizeStatusFilter(String status) {
        if (status == null || status.isBlank() || status.equalsIgnoreCase("ALL")) {
            return null;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!normalized.equals("ACTIVE") && !normalized.equals("SUSPENDED")) {
            throw AdminApiException.badRequest("Status filter must be ACTIVE or SUSPENDED.");
        }
        return normalized;
    }

    private String effectiveStatus(User user) {
        String status = user.getAccountStatus();
        return status == null || status.isBlank() ? "ACTIVE" : status.toUpperCase(Locale.ROOT);
    }

    private String toApiRole(String role) {
        if (role == null || role.isBlank()) {
            return "VAULT_OWNER";
        }
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        if (normalized.equals("SYSTEM_ADMIN")) {
            return "ADMIN";
        }
        if (normalized.equals("DIGITAL_EXECUTOR")) {
            return "EXECUTOR";
        }
        return normalized;
    }
}

