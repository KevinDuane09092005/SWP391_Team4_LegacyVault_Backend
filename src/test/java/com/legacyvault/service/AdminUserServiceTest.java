package com.legacyvault.service;

import com.legacyvault.dto.AdminUserPageResponse;
import com.legacyvault.dto.AdminUserResponse;
import com.legacyvault.entity.User;
import com.legacyvault.exception.AdminApiException;
import com.legacyvault.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

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

    @InjectMocks
    private AdminUserService adminUserService;

    @Test
    void listUsersMapsBackendRoleNamesAndPaginationForTheFe() {
        User executor = user("u-1", "Executor Example", "executor@example.com", "digital_executor", null);
        when(userRepository.searchAdminUsers(eq("exec"), eq("digital_executor"), eq("ACTIVE"),
                any(PageRequest.class))).thenReturn(new PageImpl<>(List.of(executor), PageRequest.of(0, 10), 1));

        AdminUserPageResponse response = adminUserService.listUsers(" exec ", "EXECUTOR", "ACTIVE", 0, 10);

        assertEquals(1, response.users().size());
        AdminUserResponse row = response.users().getFirst();
        assertEquals("u-1", row.id());
        assertEquals("EXECUTOR", row.role());
        assertEquals("ACTIVE", row.status());
        assertEquals(1, response.pagination().totalPages());
        assertNull(executor.getPasswordHash());
        verify(userRepository).searchAdminUsers(eq("exec"), eq("digital_executor"), eq("ACTIVE"),
                argThat(pageable -> pageable.getSort().getOrderFor("createdAt").getDirection() == Sort.Direction.DESC));
    }

    @Test
    void updateStatusPersistsAuditsAndNotifiesTheTargetUser() {
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
        assertThrows(AdminApiException.class,
                () -> adminUserService.updateStatus("admin-1", "admin-1", "SUSPENDED", null));
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
