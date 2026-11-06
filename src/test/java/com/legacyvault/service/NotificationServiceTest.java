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

    private InAppNotification mockNotification;

    @BeforeEach
    void setUp() {
        mockNotification = new InAppNotification("user-1", "Legal Verification Approved",
                "Legal verification for Case #LV-1042 has been approved by the legal verifier.",
                "VERIFICATION_RESULT", "Approved");
        mockNotification.setNotificationId("notif-1042-approved");
        mockNotification.setDeepLink("/dashboard/beneficiary/handover/LV-1042");
    }

    @Test
    void getNotificationListWithData() {
        Page<InAppNotification> page = new PageImpl<>(List.of(mockNotification));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq("user-1"), any(PageRequest.class)))
                .thenReturn(page);
        when(notificationRepository.countByUserIdAndIsReadFalse("user-1")).thenReturn(1L);

        NotificationCenterListResponse response =
                notificationService.getNotificationList("user-1", PageRequest.of(0, 10));

        assertEquals(1, response.getNotifications().size());
        assertEquals("Legal Verification Approved", response.getNotifications().get(0).getTitle());
        assertEquals("Approved", response.getNotifications().get(0).getBadgeLabel());
        assertEquals("LV-1042", response.getNotifications().get(0).getCaseId());
        assertEquals("/dashboard/beneficiary/handover/LV-1042", response.getNotifications().get(0).getDeepLink());
        assertEquals(1, response.getUnreadCount());
    }

    @Test
    void getNotificationListUsesPrototypeRowsWhenEmpty() {
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq("user-1"), any(PageRequest.class)))
                .thenReturn(Page.empty());

        NotificationCenterListResponse response =
                notificationService.getNotificationList("user-1", PageRequest.of(0, 10));

        assertEquals(5, response.getNotifications().size());
        assertEquals(2, response.getUnreadCount());
        assertEquals("LV-1042", response.getNotifications().get(0).getCaseId());
    }

    @Test
    void markAllAsReadUpdatesOnlyCurrentUsersNotifications() {
        when(notificationRepository.findByUserIdAndIsReadFalse("user-1")).thenReturn(List.of(mockNotification));

        notificationService.markAllAsRead("user-1");

        assertTrue(mockNotification.getIsRead());
        verify(notificationRepository).save(mockNotification);
    }

    @Test
    void markAsReadUpdatesSingleNotification() {
        mockNotification.setIsRead(false);
        when(notificationRepository.findByNotificationIdAndUserId("notif-1042-approved", "user-1"))
                .thenReturn(Optional.of(mockNotification));

        notificationService.markAsRead("user-1", "notif-1042-approved");

        assertTrue(mockNotification.getIsRead());
        verify(notificationRepository).save(mockNotification);
    }

    @Test
    void getPrototypeNotificationDetail() {
        when(notificationRepository.findByNotificationIdAndUserId("notif-1042-approved", "user-1"))
                .thenReturn(Optional.empty());

        NotificationDetailResponse detail =
                notificationService.getNotificationDetail("user-1", "notif-1042-approved");

        assertEquals("Legal Verification Approved", detail.getTitle());
        assertEquals("Case #LV-1042", detail.getEventMetadata().getCaseId());
        assertEquals(3, detail.getRelatedActivityTimeline().size());
    }

    @Test
    void paymentNotificationDetailIsBuiltFromTheOwnedNotificationAndAutoMarkedAsRead() {
        InAppNotification paymentNotification = new InAppNotification(
                "user-1", "Payment successful", "Your subscription renewed.", "PAYMENT_SUCCEEDED", "Success");
        paymentNotification.setNotificationId("payment-notice-1");
        paymentNotification.setIsRead(false);
        when(notificationRepository.findByNotificationIdAndUserId("payment-notice-1", "user-1"))
                .thenReturn(Optional.of(paymentNotification));

        NotificationDetailResponse detail =
                notificationService.getNotificationDetail("user-1", "payment-notice-1");

        assertEquals("Payment successful", detail.getTitle());
        assertEquals("PAYMENT_SUCCEEDED", detail.getEventMetadata().getEventType());
        assertEquals("Your subscription renewed.", detail.getRelatedActivityTimeline().get(0).getDescription());
        assertTrue(paymentNotification.getIsRead());
        verify(notificationRepository).save(paymentNotification);
    }

    @Test
    void notificationDetailsCannotBeReadByAnotherUser() {
        when(notificationRepository.findByNotificationIdAndUserId("payment-notice-1", "user-2"))
                .thenReturn(Optional.empty());

        assertThrows(BillingException.class,
                () -> notificationService.getNotificationDetail("user-2", "payment-notice-1"));
    }

    @Test
    void createNotificationPersistsIntoTheSharedInbox() {
        when(notificationRepository.save(any(InAppNotification.class))).thenReturn(mockNotification);

        InAppNotification created = notificationService.createNotification(
                "user-1", "Title", "Content", "PAYMENT_SUCCEEDED", "Success", "/pricing");

        assertNotNull(created);
        verify(notificationRepository).save(any(InAppNotification.class));
    }

    @Test
    void extractCaseIdFromVariousFormats() {
        assertEquals("LV-1042", NotificationService.extractCaseId("/dashboard/beneficiary/handover/LV-1042", null, null));
        assertEquals("LV-2048", NotificationService.extractCaseId(null, "Handover Case #LV-2048 Triggered", null));
        assertEquals("CASE-99", NotificationService.extractCaseId(null, null, "Details for case #CASE-99 are ready"));
        assertNull(NotificationService.extractCaseId(null, "Welcome to LegacyVault", "No case reference here"));
    }
}
