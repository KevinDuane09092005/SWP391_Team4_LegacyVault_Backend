package com.legacyvault.service;

import com.legacyvault.dto.AdminUserPageResponse;
import com.legacyvault.dto.AdminUserResponse;
import com.legacyvault.entity.User;
import com.legacyvault.exception.AdminApiException;
import com.legacyvault.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private NotificationService notificationService;

    private AdminUserService adminUserService;

    @BeforeEach
    void setUp() {
        adminUserService = new AdminUserService(userRepository, auditLogService, notificationService);
    }

    @Test
    void listUsersMapsStoredExecutorRoleToFrontendRole() {
        User executor = user("u-1", "Minh Le", "executor@example.com", "digital_executor", "ACTIVE");
        when(userRepository.searchAdminUsers(eq("minh"), eq("digital_executor"), eq("ACTIVE"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(executor)));

        AdminUserPageResponse response = adminUserService.listUsers("minh", "EXECUTOR", "ACTIVE", 0, 20);

        assertEquals(1, response.users().size());
        assertEquals("EXECUTOR", response.users().get(0).role());
        assertEquals("ACTIVE", response.users().get(0).status());
    }

    @Test
    void updateStatusSuspendsUserAndWritesAuditAndNotification() {
        User admin = user("admin-1", "Admin User", "admin@example.com", "admin", "ACTIVE");
        User target = user("u-1", "Target User", "target@example.com", "vault_owner", null);
        when(userRepository.findById("u-1")).thenReturn(Optional.of(target));
        when(userRepository.findById("admin-1")).thenReturn(Optional.of(admin));
        when(userRepository.save(target)).thenReturn(target);

        AdminUserResponse response = adminUserService.updateStatus("admin-1", "u-1", "SUSPENDED", "127.0.0.1");

        assertEquals("SUSPENDED", response.status());
        assertEquals("SUSPENDED", target.getAccountStatus());
        verify(auditLogService).recordLog("admin-1", "Admin User", "ADMIN", "USER_STATUS_CHANGED",
                "User u-1", "SUCCESS", "127.0.0.1", "Changed USER_STATUS_CHANGED from ACTIVE to SUSPENDED");
        verify(notificationService).createNotification("u-1", "Account status updated",
                "An administrator changed your account status to SUSPENDED.", "ACCOUNT_STATUS_CHANGED", "SUSPENDED", null);
    }

    @Test
    void updateRoleAcceptsFeExecutorNameAndStoresBackendRole() {
        User admin = user("admin-1", "Admin User", "admin@example.com", "admin", "ACTIVE");
        User target = user("u-1", "Target User", "target@example.com", "vault_owner", "ACTIVE");
        when(userRepository.findById("u-1")).thenReturn(Optional.of(target));
        when(userRepository.findById("admin-1")).thenReturn(Optional.of(admin));
        when(userRepository.save(target)).thenReturn(target);

        AdminUserResponse response = adminUserService.updateRole("admin-1", "u-1", "EXECUTOR", null);

        assertEquals("digital_executor", target.getRole());
        assertEquals("EXECUTOR", response.role());
        verify(auditLogService).recordLog("admin-1", "Admin User", "ADMIN", "USER_ROLE_CHANGED",
                "User u-1", "SUCCESS", null, "Changed USER_ROLE_CHANGED from VAULT_OWNER to EXECUTOR");
    }

    @Test
    void adminsCannotChangeTheirOwnAccount() {
        AdminApiException ex = assertThrows(AdminApiException.class,
                () -> adminUserService.updateStatus("admin-1", "admin-1", "SUSPENDED", null));
        assertEquals(400, ex.getStatus().value());
        verify(userRepository, never()).findById(anyString());
    }

    private User user(String id, String name, String email, String role, String status) {
        User user = new User();
        user.setId(id);
        user.setFullName(name);
        user.setEmail(email);
        user.setRole(role);
        user.setAccountStatus(status);
        return user;
    }
}

