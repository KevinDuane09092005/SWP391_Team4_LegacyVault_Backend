package com.legacyvault.service;

import com.legacyvault.dto.AdminDashboardResponse;
import com.legacyvault.entity.AccessRequest;
import com.legacyvault.entity.User;
import com.legacyvault.exception.AdminApiException;
import com.legacyvault.repository.AccessRequestRepository;
import com.legacyvault.repository.AuditLogRepository;
import com.legacyvault.repository.InAppNotificationRepository;
import com.legacyvault.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccessRequestRepository accessRequestRepository;

    @Mock
    private InAppNotificationRepository notificationRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AdminDashboardService adminDashboardService;

    private AccessRequest mockRequest;

    @BeforeEach
    void setUp() {
        mockRequest = new AccessRequest("Anthony Walker", "anthony@executor.com",
                "DIGITAL_EXECUTOR", "Legal Executor, Case #LV-1042");
        mockRequest.setRequestId("req-01");
    }

    @Test
    void testGetAdminDashboard_CountsAndModules() {
        when(userRepository.count()).thenReturn(24L);
        when(accessRequestRepository.countByStatus("PENDING")).thenReturn(3L);
        when(notificationRepository.count()).thenReturn(156L);
        when(auditLogRepository.count()).thenReturn(1247L);
        when(userRepository.findAll()).thenReturn(Collections.emptyList());
        when(accessRequestRepository.findByStatusOrderByCreatedAtDesc("PENDING")).thenReturn(List.of(mockRequest));

        AdminDashboardResponse dashboard = adminDashboardService.getAdminDashboard();

        assertNotNull(dashboard);
        assertEquals(24, dashboard.getKpiMetrics().getTotalUsers());
        assertEquals(3, dashboard.getKpiMetrics().getPendingRequests());
        assertEquals(156, dashboard.getKpiMetrics().getNotificationsSent());
        assertEquals(1247, dashboard.getKpiMetrics().getAuditEvents());
        assertEquals(1, dashboard.getAccessRequestsQueue().size());
        assertEquals(5, dashboard.getRolesAndPermissionsSummary().size());
        assertTrue(dashboard.getSystemSettingsModules().isEmpty());
    }

    @Test
    void testApproveAccessRequest() {
        User admin = new User();
        admin.setId("admin-1");
        admin.setFullName("Admin User");
        admin.setRole("admin");
        when(accessRequestRepository.findById("req-01")).thenReturn(Optional.of(mockRequest));
        when(userRepository.findById("admin-1")).thenReturn(Optional.of(admin));

        adminDashboardService.approveAccessRequest("req-01", "admin-1");

        assertEquals("APPROVED", mockRequest.getStatus());
        assertEquals("admin-1", mockRequest.getReviewedBy());
        verify(accessRequestRepository).save(mockRequest);
        verify(auditLogService).recordLog(eq("admin-1"), eq("Admin User"), eq("ADMIN"),
                eq("ACCESS_REQUEST_APPROVED"), eq("Access request req-01"), eq("SUCCESS"), isNull(), anyString());
    }

    @Test
    void testRejectAccessRequest() {
        User admin = new User();
        admin.setId("admin-1");
        admin.setFullName("Admin User");
        admin.setRole("admin");
        when(accessRequestRepository.findById("req-01")).thenReturn(Optional.of(mockRequest));
        when(userRepository.findById("admin-1")).thenReturn(Optional.of(admin));

        adminDashboardService.rejectAccessRequest("req-01", "admin-1");

        assertEquals("REJECTED", mockRequest.getStatus());
        assertEquals("admin-1", mockRequest.getReviewedBy());
        verify(accessRequestRepository).save(mockRequest);
        verify(auditLogService).recordLog(eq("admin-1"), eq("Admin User"), eq("ADMIN"),
                eq("ACCESS_REQUEST_REJECTED"), eq("Access request req-01"), eq("SUCCESS"), isNull(), anyString());
    }

    @Test
    void testUnknownAccessRequestIsNotSilentlyApproved() {
        when(accessRequestRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(AdminApiException.class,
                () -> adminDashboardService.approveAccessRequest("missing", "admin-1"));
        verify(accessRequestRepository, never()).save(any());
    }
}
