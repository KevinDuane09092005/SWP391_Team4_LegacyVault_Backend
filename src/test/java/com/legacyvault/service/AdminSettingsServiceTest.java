package com.legacyvault.service;

import com.legacyvault.entity.SystemPolicy;
import com.legacyvault.entity.ThirdPartyIntegration;
import com.legacyvault.entity.User;
import com.legacyvault.exception.AdminApiException;
import com.legacyvault.repository.SystemPolicyRepository;
import com.legacyvault.repository.ThirdPartyIntegrationRepository;
import com.legacyvault.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminSettingsServiceTest {

    @Mock
    private SystemPolicyRepository systemPolicyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ThirdPartyIntegrationRepository thirdPartyIntegrationRepository;

    private AdminSettingsService adminSettingsService;

    @BeforeEach
    void setUp() {
        adminSettingsService = new AdminSettingsService(
                systemPolicyRepository, userRepository, auditLogService, notificationService, thirdPartyIntegrationRepository);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getPermissionsReturnsDefaultMatrixAndLockedAdminPermissions() {
        when(systemPolicyRepository.findAll()).thenReturn(List.of());

        Map<String, Object> result = adminSettingsService.getPermissions();

        List<String> permissions = (List<String>) result.get("permissions");
        Map<String, List<String>> matrix = (Map<String, List<String>>) result.get("matrix");
        Map<String, List<String>> locked = (Map<String, List<String>>) result.get("locked");

        assertEquals(9, permissions.size());
        assertTrue(matrix.get("SYSTEM_ADMIN").contains("admin.dashboard"));
        assertTrue(matrix.get("VAULT_OWNER").contains("vault.manage"));
        assertTrue(locked.get("SYSTEM_ADMIN").contains("users.manage"));
    }

    @Test
    void lockedAdminPermissionCannotBeRemoved() {
        AdminApiException ex = assertThrows(AdminApiException.class,
                () -> adminSettingsService.updatePermission("admin-1", "SYSTEM_ADMIN", "admin.dashboard", false, "127.0.0.1"));
        assertEquals("LOCKED_PERMISSION", ex.getCode());
    }

    @Test
    void updatePermissionNotifiesAffectedUsers() {
        User verifier = new User();
        verifier.setId("ver-1");
        verifier.setRole("legal_verifier");
        when(systemPolicyRepository.findAll()).thenReturn(List.of());
        when(systemPolicyRepository.findById(anyString())).thenReturn(Optional.empty());
        when(userRepository.findAll()).thenReturn(List.of(verifier));

        adminSettingsService.updatePermission("admin-1", "LEGAL_VERIFIER", "audit.view", true, "127.0.0.1");

        verify(notificationService).createNotification(
                eq("ver-1"),
                eq("Role permissions updated"),
                contains("audit.view"),
                eq("PERMISSIONS_CHANGED"),
                eq("Info"),
                eq("/dashboard"));
    }

    @Test
    void listIntegrationsReflectsThirdPartyIntegrationsTableState() {
        ThirdPartyIntegration aiDbRow = new ThirdPartyIntegration(
                "AI_ASSISTANT", "HEALTHY", "{\"enabled\":true,\"id\":\"ai-assistant\"}");
        when(systemPolicyRepository.findAll()).thenReturn(List.of());
        when(thirdPartyIntegrationRepository.findAll()).thenReturn(List.of(aiDbRow));

        List<Map<String, Object>> integrations = adminSettingsService.listIntegrations();
        Map<String, Object> ai = integrations.stream()
                .filter(i -> "ai-assistant".equals(i.get("id")))
                .findFirst()
                .orElseThrow();

        assertEquals(true, ai.get("enabled"));
        assertEquals("HEALTHY", ai.get("connectionStatus"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getPoliciesAndSavePoliciesWorkCorrectly() {
        when(systemPolicyRepository.findAll()).thenReturn(List.of());
        when(systemPolicyRepository.findById(anyString())).thenReturn(Optional.empty());

        Map<String, Object> initial = adminSettingsService.getPolicies();
        Map<String, Object> policies = (Map<String, Object>) initial.get("policies");
        assertEquals("AES-256-GCM", policies.get("encryptionAlgorithm"));
        assertEquals(90, policies.get("keyRotationDays"));

        Map<String, Object> updatePayload = new HashMap<>(policies);
        updatePayload.put("keyRotationDays", 60);
        adminSettingsService.savePolicies("admin-1", updatePayload, "127.0.0.1");

        verify(systemPolicyRepository, atLeastOnce()).save(any(SystemPolicy.class));
        verify(auditLogService).recordLog(eq("admin-1"), anyString(), eq("ADMIN"),
                eq("POLICY_CHANGED"), eq("POLICY encryption-backup"), eq("SUCCESS"), eq("127.0.0.1"), anyString());
    }

    @Test
    void listAndToggleIntegrationsWorkCorrectly() {
        when(systemPolicyRepository.findAll()).thenReturn(List.of());
        when(systemPolicyRepository.findById("integration.sms")).thenReturn(Optional.empty());
        when(thirdPartyIntegrationRepository.findByServiceName("SMS_GATEWAY")).thenReturn(Optional.empty());
        when(systemPolicyRepository.findById("integration.ai-assistant")).thenReturn(Optional.empty());
        when(thirdPartyIntegrationRepository.findByServiceName("AI_ASSISTANT")).thenReturn(Optional.empty());

        List<Map<String, Object>> integrations = adminSettingsService.listIntegrations();
        assertEquals(7, integrations.size());
        assertEquals("email", integrations.get(0).get("id"));
        assertEquals(true, integrations.get(0).get("enabled"));
        assertEquals("ai-assistant", integrations.get(6).get("id"));
        assertEquals(false, integrations.get(6).get("enabled"));

        adminSettingsService.updateIntegration("admin-1", "sms", true, "127.0.0.1");
        verify(systemPolicyRepository).save(any(SystemPolicy.class));
        verify(auditLogService).recordLog(eq("admin-1"), anyString(), eq("ADMIN"),
                eq("INTEGRATION_CHANGED"), eq("INTEGRATION sms"), eq("SUCCESS"), eq("127.0.0.1"), anyString());

        assertDoesNotThrow(() -> adminSettingsService.updateIntegration("admin-1", "ai-assistant", true, "127.0.0.1"));
        verify(auditLogService).recordLog(eq("admin-1"), anyString(), eq("ADMIN"),
                eq("INTEGRATION_CHANGED"), eq("INTEGRATION ai-assistant"), eq("SUCCESS"), eq("127.0.0.1"), anyString());

        ArgumentCaptor<ThirdPartyIntegration> captor = ArgumentCaptor.forClass(ThirdPartyIntegration.class);
        verify(thirdPartyIntegrationRepository, times(2)).save(captor.capture());
        Map<String, ThirdPartyIntegration> savedByService = new HashMap<>();
        for (ThirdPartyIntegration saved : captor.getAllValues()) {
            savedByService.put(saved.getServiceName(), saved);
        }
        ThirdPartyIntegration sms = savedByService.get("SMS_GATEWAY");
        assertEquals("HEALTHY", sms.getStatus());
        assertNotNull(sms.getLastSyncedAt());

        ThirdPartyIntegration aiAssistant = savedByService.get("AI_ASSISTANT");
        assertEquals("AI_ASSISTANT", aiAssistant.getServiceName());
        assertEquals("HEALTHY", aiAssistant.getStatus());
    }
}
