package com.legacyvault.service;

import com.legacyvault.dto.NotificationCenterItem;
import com.legacyvault.dto.NotificationCenterListResponse;
import com.legacyvault.dto.NotificationDetailResponse;
import com.legacyvault.entity.InAppNotification;
import com.legacyvault.repository.InAppNotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
public class NotificationService {

    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH);
    private static final Set<String> PROTOTYPE_NOTIFICATION_IDS = Set.of(
            "notif-001", "notif-002", "notif-003", "notif-004", "notif-005", "notif-1042-approved");

    private final InAppNotificationRepository notificationRepository;

    public NotificationService(InAppNotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public NotificationCenterListResponse getNotificationList(String userId, Pageable pageable) {
        Page<InAppNotification> page = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        List<NotificationCenterItem> items = new ArrayList<>();
        if (page.isEmpty()) {
            items.add(new NotificationCenterItem("notif-001", "Legal Verification Approved",
                    "Legal verification for Case #LV-1042 has been approved by the legal verifier.",
                    "24 Sep 2026, 11:45 AM", "Approved", false));
            items.add(new NotificationCenterItem("notif-002", "Asset Handover Available",
                    "Assets for Case #LV-1042 are now available for beneficiary claim.",
                    "24 Sep 2026, 10:30 AM", "Pending", false));
            items.add(new NotificationCenterItem("notif-003", "Check-in Completed",
                    "Regular Dead Man's Switch check-in recorded successfully.",
                    "23 Sep 2026, 09:15 AM", "Verified", true));
            items.add(new NotificationCenterItem("notif-004", "New Review Request",
                    "A new case requires legal document verification.",
                    "22 Sep 2026, 02:20 PM", "New", true));
            items.add(new NotificationCenterItem("notif-005", "Access Permission Granted",
                    "Your role has been updated to Digital Executor.",
                    "20 Sep 2026, 04:00 PM", "Info", true));
            return new NotificationCenterListResponse(items, 2);
        }

        for (InAppNotification notification : page.getContent()) {
            String formattedTime = notification.getCreatedAt() == null
                    ? "Recent" : notification.getCreatedAt().format(DISPLAY_TIME);
            items.add(new NotificationCenterItem(
                    notification.getNotificationId(), notification.getTitle(), notification.getContent(),
                    formattedTime, notification.getBadgeLabel(), Boolean.TRUE.equals(notification.getIsRead())));
        }

        int unreadCount = (int) notificationRepository.countByUserIdAndIsReadFalse(userId);
        return new NotificationCenterListResponse(items, unreadCount);
    }

    @Transactional
    public void markAllAsRead(String userId) {
        List<InAppNotification> unreadNotifications = notificationRepository.findByUserIdAndIsReadFalse(userId);
        for (InAppNotification notification : unreadNotifications) {
            notification.setIsRead(true);
            notificationRepository.save(notification);
        }
    }

    @Transactional(readOnly = true)
    public NotificationDetailResponse getNotificationDetail(String userId, String notificationId) {
        Optional<InAppNotification> ownedNotification =
                notificationRepository.findByNotificationIdAndUserId(notificationId, userId);
        if (ownedNotification.isPresent()) {
            InAppNotification notification = ownedNotification.get();
            String occurredAt = notification.getCreatedAt() == null
                    ? "Recent" : notification.getCreatedAt().format(DISPLAY_TIME);
            return new NotificationDetailResponse(
                    notificationId,
                    notification.getTitle(),
                    notification.getBadgeLabel(),
                    notification.getContent(),
                    new NotificationDetailResponse.EventMetadata(
                            null, null, occurredAt, notification.getEventType(), "LegacyVault"),
                    List.of(new NotificationDetailResponse.TimelineItem(
                            notification.getTitle(), notification.getContent(), occurredAt)));
        }

        if (!PROTOTYPE_NOTIFICATION_IDS.contains(notificationId)) {
            throw BillingException.notFound();
        }
        return prototypeNotificationDetail(notificationId);
    }

    private NotificationDetailResponse prototypeNotificationDetail(String notificationId) {
        NotificationDetailResponse.EventMetadata metadata = new NotificationDetailResponse.EventMetadata(
                "Case #LV-1042", "Thanh Pham", "24 Sep 2026, 11:45 AM", "Legal Verification", "Attorney Notary");
        List<NotificationDetailResponse.TimelineItem> timeline = Arrays.asList(
                new NotificationDetailResponse.TimelineItem("Legal Verification Approved",
                        "Verification request submitted with death certificate document reviewed & approved", "24 Sep 2026, 11:45 AM"),
                new NotificationDetailResponse.TimelineItem("Document Review In Progress",
                        "Legal verifier assigned and began examining submitted death certificate", "24 Sep 2026, 10:15 AM"),
                new NotificationDetailResponse.TimelineItem("Handover Case Triggered",
                        "Dead Man's Switch timer expired without check-in response", "23 Sep 2026, 08:00 AM"));
        return new NotificationDetailResponse(notificationId, "Legal Verification Approved", "Approved",
                "The legal verification for Case #LV-1042 has been reviewed and approved by the legal verification team. "
                        + "All documentation has been verified and the case has been moved to Release & Delivery stage for processing.",
                metadata, timeline);
    }

    @Transactional
    public InAppNotification createNotification(String userId, String title, String content,
                                                String eventType, String badgeLabel, String deepLink) {
        InAppNotification notification = new InAppNotification(userId, title, content, eventType, badgeLabel);
        notification.setDeepLink(deepLink);
        return notificationRepository.save(notification);
    }
}
