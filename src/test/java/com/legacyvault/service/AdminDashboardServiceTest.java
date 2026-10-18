package com.legacyvault.service;

import com.legacyvault.dto.AdminDashboardResponse;
import com.legacyvault.entity.AccessRequest;
import com.legacyvault.entity.User;
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
        when(accessRequestRepository.findByStatus("PENDING")).thenReturn(List.of(mockRequest));

        AdminDashboardResponse dashboard = adminDashboardService.getAdminDashboard();

        assertNotNull(dashboard);
        assertEquals(24, dashboard.getKpiMetrics().getTotalUsers());
        assertEquals(3, dashboard.getKpiMetrics().getPendingRequests());
        assertEquals(156, dashboard.getKpiMetrics().getNotificationsSent());
        assertEquals(1247, dashboard.getKpiMetrics().getAuditEvents());
        assertEquals(1, dashboard.getAccessRequestsQueue().size());
        assertEquals(4, dashboard.getRolesAndPermissionsSummary().size());
        assertEquals(4, dashboard.getSystemSettingsModules().size());
    }

    @Test
    void testApproveAccessRequest() {
        when(accessRequestRepository.findById("req-01")).thenReturn(Optional.of(mockRequest));

        adminDashboardService.approveAccessRequest("req-01", "admin-1");

        assertEquals("APPROVED", mockRequest.getStatus());
        assertEquals("admin-1", mockRequest.getReviewedBy());
        verify(accessRequestRepository).save(mockRequest);
    }

    @Test
    void testRejectAccessRequest() {
        when(accessRequestRepository.findById("req-01")).thenReturn(Optional.of(mockRequest));

        adminDashboardService.rejectAccessRequest("req-01", "admin-1");

        assertEquals("REJECTED", mockRequest.getStatus());
        assertEquals("admin-1", mockRequest.getReviewedBy());
        verify(accessRequestRepository).save(mockRequest);
    }
}
