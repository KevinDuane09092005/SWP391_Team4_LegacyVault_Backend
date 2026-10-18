package com.legacyvault.service;

import com.legacyvault.dto.NotificationCenterListResponse;
import com.legacyvault.dto.NotificationDetailResponse;
import com.legacyvault.entity.InAppNotification;
import com.legacyvault.repository.InAppNotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private InAppNotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    private InAppNotification mockNotif;

    @BeforeEach
    void setUp() {
        mockNotif = new InAppNotification("user-1", "Legal Verification Approved",
                "Legal verification for Case #LV-1042 has been approved by the legal verifier.",
                "VERIFICATION_RESULT", "Approved");
        mockNotif.setNotificationId("notif-1042-approved");
    }

    @Test
    void testGetNotificationList_WithData() {
        Page<InAppNotification> page = new PageImpl<>(List.of(mockNotif));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq("user-1"), any(PageRequest.class)))
                .thenReturn(page);
        when(notificationRepository.countByUserIdAndIsReadFalse("user-1")).thenReturn(1L);

        NotificationCenterListResponse response = notificationService.getNotificationList("user-1", PageRequest.of(0, 10));

        assertNotNull(response);
        assertEquals(1, response.getNotifications().size());
        assertEquals("Legal Verification Approved", response.getNotifications().get(0).getTitle());
        assertEquals("Approved", response.getNotifications().get(0).getBadgeLabel());
        assertEquals(1, response.getUnreadCount());
    }

    @Test
    void testGetNotificationList_EmptyFallback() {
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq("user-1"), any(PageRequest.class)))
                .thenReturn(Page.empty());

        NotificationCenterListResponse response = notificationService.getNotificationList("user-1", PageRequest.of(0, 10));

        assertNotNull(response);
        assertFalse(response.getNotifications().isEmpty());
        assertEquals(5, response.getNotifications().size());
    }

    @Test
    void testMarkAllAsRead() {
        when(notificationRepository.findByUserIdAndIsReadFalse("user-1")).thenReturn(List.of(mockNotif));

        notificationService.markAllAsRead("user-1");

        assertTrue(mockNotif.getIsRead());
        verify(notificationRepository).save(mockNotif);
    }

    @Test
    void testGetNotificationDetail() {
        when(notificationRepository.findById("notif-1042-approved")).thenReturn(Optional.of(mockNotif));

        NotificationDetailResponse detail = notificationService.getNotificationDetail("notif-1042-approved");

        assertNotNull(detail);
        assertEquals("Legal Verification Approved", detail.getTitle());
        assertEquals("Case #LV-1042", detail.getEventMetadata().getCaseId());
        assertEquals(3, detail.getRelatedActivityTimeline().size());
    }

    @Test
    void testCreateNotification() {
        when(notificationRepository.save(any(InAppNotification.class))).thenReturn(mockNotif);

        InAppNotification created = notificationService.createNotification(
                "user-1", "Title", "Content", "TEST_EVENT", "New", "/link");

        assertNotNull(created);
        verify(notificationRepository).save(any(InAppNotification.class));
    }
}
