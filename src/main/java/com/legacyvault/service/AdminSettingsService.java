package com.legacyvault.service;

import com.legacyvault.entity.SystemPolicy;
import com.legacyvault.entity.User;
import com.legacyvault.exception.AdminApiException;
import com.legacyvault.repository.SystemPolicyRepository;
import com.legacyvault.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class AdminSettingsService {

    public static final List<String> PERMISSIONS = List.of(
            "admin.dashboard",
            "audit.view",
            "users.manage",
            "vault.manage",
            "assignments.manage",
            "handover.start",
            "handover.receive",
            "legal.review",
            "notifications.view"
    );

    public static final List<String> LOCKED_ADMIN_PERMISSIONS = List.of("admin.dashboard", "users.manage");

    private static final Map<String, List<String>> DEFAULT_ROLE_PERMISSIONS = Map.of(
            "SYSTEM_ADMIN", List.of("admin.dashboard", "audit.view", "users.manage", "notifications.view"),
            "VAULT_OWNER", List.of("vault.manage", "assignments.manage", "notifications.view"),
            "DIGITAL_EXECUTOR", List.of("handover.start", "notifications.view"),
            "BENEFICIARY", List.of("handover.receive", "notifications.view"),
            "LEGAL_VERIFIER", List.of("legal.review", "notifications.view")
    );

    private static final List<String> ENCRYPTION_ALGORITHMS = List.of("AES-256-GCM", "XChaCha20-Poly1305");
    private static final List<String> BACKUP_FREQUENCIES = List.of("HOURLY", "DAILY", "WEEKLY");
    private static final List<String> BACKUP_REGIONS = List.of("ap-southeast-1", "eu-west-1", "us-east-1");

    private final SystemPolicyRepository systemPolicyRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    public AdminSettingsService(SystemPolicyRepository systemPolicyRepository,
                                UserRepository userRepository,
                                AuditLogService auditLogService,
                                NotificationService notificationService) {
        this.systemPolicyRepository = systemPolicyRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    // ---------------- 5. Roles & Permissions ----------------

    @Transactional(readOnly = true)
    public Map<String, Object> getPermissions() {
        Map<String, List<String>> matrix = loadPermissionMatrix();
        Map<String, List<String>> locked = new LinkedHashMap<>();
        locked.put("SYSTEM_ADMIN", LOCKED_ADMIN_PERMISSIONS);
        locked.put("ADMIN", LOCKED_ADMIN_PERMISSIONS);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("permissions", PERMISSIONS);
        response.put("matrix", matrix);
        response.put("locked", locked);
        return response;
    }

    @Transactional
    public Map<String, List<String>> updatePermission(String adminId, String role, String permission, Boolean granted, String ipAddress) {
        if (role == null || role.isBlank() || permission == null || permission.isBlank() || granted == null) {
            throw AdminApiException.badRequest("role, permission, and granted are required.");
        }
        String canonicalRole = normalizeCanonicalRole(role);
        if (canonicalRole == null || !PERMISSIONS.contains(permission.trim())) {
            throw AdminApiException.badRequest("Invalid role or permission.");
        }
        String cleanPerm = permission.trim();
        if (!granted && "SYSTEM_ADMIN".equals(canonicalRole) && LOCKED_ADMIN_PERMISSIONS.contains(cleanPerm)) {
            throw AdminApiException.badRequest("Locked administrator permissions cannot be removed.");
        }

        Map<String, List<String>> matrix = loadPermissionMatrix();
        Set<String> current = new LinkedHashSet<>(matrix.getOrDefault(canonicalRole, List.of()));
        if (granted) {
            current.add(cleanPerm);
        } else {
            current.remove(cleanPerm);
        }
        List<String> updated = PERMISSIONS.stream().filter(current::contains).toList();

        savePolicyValue("permissions.role." + canonicalRole, String.join(",", updated),
                "Role permissions for " + canonicalRole, adminId);

        recordAdminAudit(adminId, "PERMISSION_CHANGED", "Role " + canonicalRole, ipAddress,
                "Permission " + cleanPerm + " set to " + granted + " for " + canonicalRole);

        return loadPermissionMatrix();
    }

    private Map<String, List<String>> loadPermissionMatrix() {
        Map<String, String> savedMap = loadAllPolicyValues();
        Map<String, List<String>> matrix = new LinkedHashMap<>();

        for (String canonicalRole : List.of("SYSTEM_ADMIN", "VAULT_OWNER", "DIGITAL_EXECUTOR", "BENEFICIARY", "LEGAL_VERIFIER")) {
            String key = "permissions.role." + canonicalRole;
            List<String> perms;
            if (savedMap.containsKey(key)) {
                String raw = savedMap.get(key);
                if (raw == null || raw.isBlank()) {
                    perms = List.of();
                } else {
                    Set<String> parts = new HashSet<>(Arrays.asList(raw.split(",")));
                    perms = PERMISSIONS.stream().filter(parts::contains).toList();
                }
            } else {
                perms = DEFAULT_ROLE_PERMISSIONS.get(canonicalRole);
            }
            matrix.put(canonicalRole, perms);
            if ("SYSTEM_ADMIN".equals(canonicalRole)) {
                matrix.put("ADMIN", perms);
            } else if ("DIGITAL_EXECUTOR".equals(canonicalRole)) {
                matrix.put("EXECUTOR", perms);
            }
        }
        return matrix;
    }

    private String normalizeCanonicalRole(String role) {
        String r = role.trim().toUpperCase(Locale.ROOT);
        return switch (r) {
            case "ADMIN", "SYSTEM_ADMIN" -> "SYSTEM_ADMIN";
            case "VAULT_OWNER" -> "VAULT_OWNER";
            case "EXECUTOR", "DIGITAL_EXECUTOR" -> "DIGITAL_EXECUTOR";
            case "BENEFICIARY" -> "BENEFICIARY";
            case "LEGAL_VERIFIER" -> "LEGAL_VERIFIER";
            default -> null;
        };
    }

    // ---------------- 6. Encryption, Backup & DMS Policies ----------------

    @Transactional(readOnly = true)
    public Map<String, Object> getPolicies() {
        Map<String, Object> policies = loadCurrentPolicies();
        Map<String, List<String>> options = new LinkedHashMap<>();
        options.put("encryptionAlgorithm", ENCRYPTION_ALGORITHMS);
        options.put("backupFrequency", BACKUP_FREQUENCIES);
        options.put("backupRegion", BACKUP_REGIONS);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("policies", policies);
        response.put("options", options);
        return response;
    }

    @Transactional
    public Map<String, Object> savePolicies(String adminId, Map<String, Object> payload, String ipAddress) {
        if (payload == null) {
            throw AdminApiException.badRequest("Policy payload is required.");
        }
        Map<String, Object> current = loadCurrentPolicies();

        String encryptionAlgorithm = getString(payload, "encryptionAlgorithm", (String) current.get("encryptionAlgorithm"));
        int keyRotationDays = getInt(payload, "keyRotationDays", (Integer) current.get("keyRotationDays"));
        boolean requireClientSideEncryption = getBoolean(payload, "requireClientSideEncryption", (Boolean) current.get("requireClientSideEncryption"));
        String backupFrequency = getString(payload, "backupFrequency", (String) current.get("backupFrequency"));
        int backupRetentionDays = getInt(payload, "backupRetentionDays", (Integer) current.get("backupRetentionDays"));
        String backupRegion = getString(payload, "backupRegion", (String) current.get("backupRegion"));
        int dmsDefaultIntervalDays = getInt(payload, "dmsDefaultIntervalDays", (Integer) current.get("dmsDefaultIntervalDays"));
        int dmsDefaultGraceDays = getInt(payload, "dmsDefaultGraceDays", (Integer) current.get("dmsDefaultGraceDays"));
        int dmsMaxReminders = getInt(payload, "dmsMaxReminders", (Integer) current.get("dmsMaxReminders"));

        if (!ENCRYPTION_ALGORITHMS.contains(encryptionAlgorithm)
                || !BACKUP_FREQUENCIES.contains(backupFrequency)
                || !BACKUP_REGIONS.contains(backupRegion)
                || keyRotationDays < 7 || keyRotationDays > 365
                || backupRetentionDays < 1 || backupRetentionDays > 3650
                || dmsDefaultIntervalDays < 7 || dmsDefaultIntervalDays > 365
                || dmsDefaultGraceDays < 1 || dmsDefaultGraceDays > 90
                || dmsMaxReminders < 1 || dmsMaxReminders > 10) {
            throw AdminApiException.badRequest("One or more policy values are outside the allowed range.");
        }

        savePolicyValue("policy.encryptionAlgorithm", encryptionAlgorithm, "Encryption algorithm", adminId);
        savePolicyValue("policy.keyRotationDays", String.valueOf(keyRotationDays), "Key rotation days", adminId);
        savePolicyValue("policy.requireClientSideEncryption", String.valueOf(requireClientSideEncryption), "Require client-side encryption", adminId);
        savePolicyValue("policy.backupFrequency", backupFrequency, "Backup frequency", adminId);
        savePolicyValue("policy.backupRetentionDays", String.valueOf(backupRetentionDays), "Backup retention days", adminId);
        savePolicyValue("policy.backupRegion", backupRegion, "Backup region", adminId);
        savePolicyValue("policy.dmsDefaultIntervalDays", String.valueOf(dmsDefaultIntervalDays), "DMS default interval days", adminId);
        savePolicyValue("policy.dmsDefaultGraceDays", String.valueOf(dmsDefaultGraceDays), "DMS default grace days", adminId);
        savePolicyValue("policy.dmsMaxReminders", String.valueOf(dmsMaxReminders), "DMS max reminders", adminId);

        recordAdminAudit(adminId, "POLICY_CHANGED", "POLICY encryption-backup", ipAddress,
                "Updated encryption, backup, and DMS policies");

        return loadCurrentPolicies();
    }

    private Map<String, Object> loadCurrentPolicies() {
        Map<String, String> saved = loadAllPolicyValues();
        Map<String, Object> policies = new LinkedHashMap<>();
        policies.put("encryptionAlgorithm", saved.getOrDefault("policy.encryptionAlgorithm", "AES-256-GCM"));
        policies.put("keyRotationDays", parseIntOrDefault(saved.get("policy.keyRotationDays"), 90));
        policies.put("requireClientSideEncryption", parseBooleanOrDefault(saved.get("policy.requireClientSideEncryption"), true));
        policies.put("backupFrequency", saved.getOrDefault("policy.backupFrequency", "DAILY"));
        policies.put("backupRetentionDays", parseIntOrDefault(saved.get("policy.backupRetentionDays"), 30));
        policies.put("backupRegion", saved.getOrDefault("policy.backupRegion", "ap-southeast-1"));
        policies.put("dmsDefaultIntervalDays", parseIntOrDefault(saved.get("policy.dmsDefaultIntervalDays"), 30));
        policies.put("dmsDefaultGraceDays", parseIntOrDefault(saved.get("policy.dmsDefaultGraceDays"), 14));
        policies.put("dmsMaxReminders", parseIntOrDefault(saved.get("policy.dmsMaxReminders"), 3));
        return policies;
    }

    // ---------------- 7. Third-Party Integrations ----------------

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listIntegrations() {
        Map<String, String> saved = loadAllPolicyValues();
        List<Map<String, Object>> defaults = List.of(
                integrationItem("email", "E-mail provider", "NOTIFICATION", true, saved),
                integrationItem("sms", "SMS gateway", "NOTIFICATION", false, saved),
                integrationItem("kyc", "KYC / eKYC provider", "IDENTITY", true, saved),
                integrationItem("storage", "Cloud backup storage", "BACKUP", true, saved),
                integrationItem("esign", "Digital signature service", "LEGAL", false, saved),
                integrationItem("enotary", "Electronic notary portal", "LEGAL", false, saved)
        );
        return defaults;
    }

    @Transactional
    public List<Map<String, Object>> updateIntegration(String adminId, String id, Boolean enabled, String ipAddress) {
        if (id == null || id.isBlank() || enabled == null) {
            throw AdminApiException.badRequest("Integration id and enabled status are required.");
        }
        String cleanId = id.trim().toLowerCase(Locale.ROOT);
        Set<String> validIds = Set.of("email", "sms", "kyc", "storage", "esign", "enotary");
        if (!validIds.contains(cleanId)) {
            throw AdminApiException.notFound("Integration was not found.");
        }

        savePolicyValue("integration." + cleanId, String.valueOf(enabled), "Integration " + cleanId + " enabled status", adminId);
        recordAdminAudit(adminId, "INTEGRATION_CHANGED", "INTEGRATION " + cleanId, ipAddress,
                "Integration " + cleanId + " enabled set to " + enabled);

        return listIntegrations();
    }

    private Map<String, Object> integrationItem(String id, String name, String category, boolean defaultEnabled, Map<String, String> saved) {
        boolean enabled = parseBooleanOrDefault(saved.get("integration." + id), defaultEnabled);
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("name", name);
        item.put("category", category);
        item.put("enabled", enabled);
        return item;
    }

    // ---------------- Helpers ----------------

    private Map<String, String> loadAllPolicyValues() {
        Map<String, String> map = new HashMap<>();
        for (SystemPolicy policy : systemPolicyRepository.findAll()) {
            map.put(policy.getPolicyKey(), policy.getPolicyValue());
        }
        return map;
    }

    private void savePolicyValue(String key, String value, String description, String adminId) {
        SystemPolicy policy = systemPolicyRepository.findById(key)
                .orElseGet(() -> new SystemPolicy(key, value, description));
        policy.setPolicyValue(value);
        policy.setDescription(description);
        if (adminId != null && userRepository.existsById(adminId)) {
            policy.setUpdatedBy(adminId);
        }
        systemPolicyRepository.save(policy);
    }

    private void recordAdminAudit(String adminId, String action, String target, String ipAddress, String detail) {
        String adminName = "System Administrator";
        String adminRole = "ADMIN";
        if (adminId != null) {
            Optional<User> adminOpt = userRepository.findById(adminId);
            if (adminOpt.isPresent()) {
                adminName = adminOpt.get().getFullName();
                adminRole = "ADMIN";
            }
        }
        auditLogService.recordLog(adminId, adminName, adminRole, action, target, "SUCCESS", ipAddress, detail);
    }

    private int parseIntOrDefault(String raw, int fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private boolean parseBooleanOrDefault(String raw, boolean fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        return Boolean.parseBoolean(raw.trim());
    }

    private String getString(Map<String, Object> map, String key, String fallback) {
        Object val = map.get(key);
        return val == null ? fallback : String.valueOf(val).trim();
    }

    private int getInt(Map<String, Object> map, String key, int fallback) {
        Object val = map.get(key);
        if (val == null) return fallback;
        if (val instanceof Number n) {
            if (n.doubleValue() != n.intValue()) {
                throw AdminApiException.badRequest(key + " must be an integer.");
            }
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(val).trim());
        } catch (NumberFormatException e) {
            throw AdminApiException.badRequest(key + " must be an integer.");
        }
    }

    private boolean getBoolean(Map<String, Object> map, String key, boolean fallback) {
        Object val = map.get(key);
        if (val == null) return fallback;
        if (val instanceof Boolean b) return b;
        String s = String.valueOf(val).trim();
        if ("true".equalsIgnoreCase(s)) return true;
        if ("false".equalsIgnoreCase(s)) return false;
        throw AdminApiException.badRequest(key + " must be a boolean.");
    }
}

