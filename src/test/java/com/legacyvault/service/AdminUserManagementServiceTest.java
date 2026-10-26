package com.legacyvault.service;

import com.legacyvault.entity.User;
import com.legacyvault.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserManagementServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private NotificationService notificationService;

    private AdminUserManagementService service;
    private User target;

    @BeforeEach
    void setUp() {
        service = new AdminUserManagementService(userRepository, auditLogService, notificationService);
        target = new User();
        target.setId("user-1");
        target.setFullName("Linh Pham");
        target.setEmail("linh@example.com");
        target.setRole("vault_owner");
        target.setIsVerified(true);
        target.setIsActive(true);
    }

    @Test
    void roleChangePersistsNotifiesAndAudits() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(target));
        when(userRepository.findById("admin-1")).thenReturn(Optional.empty());
        when(userRepository.save(target)).thenReturn(target);

        var response = service.updateRole("admin-1", "user-1", "LEGAL_VERIFIER");

        assertEquals("LEGAL_VERIFIER", target.getRole());
        assertEquals("LEGAL_VERIFIER", response.role());
        verify(userRepository).save(target);
        verify(auditLogService).recordLog(eq("admin-1"), isNull(), eq("ADMIN"),
                eq("ADMIN_USER_ROLE_CHANGED"), eq("user-1"), eq("SUCCESS"), isNull(), anyString());
        verify(notificationService).createNotification(eq("user-1"), eq("Account role updated"),
                contains("LEGAL_VERIFIER"), eq("ACCOUNT_ROLE_CHANGED"), eq("Action required"), isNull());
    }

    @Test
    void userListReturnsDatabaseUsersAndPagination() {
        when(userRepository.searchAdminUsers(eq("linh"), eq("VAULT_OWNER"), eq("ACTIVE"),
                any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(target), PageRequest.of(0, 20), 1));

        var response = service.listUsers("linh", "VAULT_OWNER", "ACTIVE", 0, 20);

        assertEquals(1, response.users().size());
        assertEquals("linh@example.com", response.users().get(0).email());
        assertEquals("ACTIVE", response.users().get(0).status());
        assertEquals(1, response.pagination().totalElements());
    }

    @Test
    void administratorCannotSuspendThemself() {
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.updateStatus("admin-1", "admin-1", "SUSPENDED"));
        verifyNoInteractions(userRepository, auditLogService, notificationService);
    }

    @Test
    void lastAdministratorCannotBeDemoted() {
        target.setRole("ADMIN");
        when(userRepository.findById("user-1")).thenReturn(Optional.of(target));
        when(userRepository.countByRoleIgnoreCase("ADMIN")).thenReturn(1L);

        var exception = assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.updateRole("admin-2", "user-1", "VAULT_OWNER"));

        assertEquals(org.springframework.http.HttpStatus.CONFLICT, exception.getStatusCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void suspendingUserPersistsInactiveStatus() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(target));
        when(userRepository.findById("admin-1")).thenReturn(Optional.empty());
        when(userRepository.save(target)).thenReturn(target);

        var response = service.updateStatus("admin-1", "user-1", "SUSPENDED");

        assertFalse(target.getIsActive());
        assertEquals("SUSPENDED", response.status());
        verify(userRepository).save(target);
        verify(auditLogService).recordLog(eq("admin-1"), isNull(), eq("ADMIN"),
                eq("ADMIN_USER_STATUS_CHANGED"), eq("user-1"), eq("SUCCESS"), isNull(), anyString());
    }
}
