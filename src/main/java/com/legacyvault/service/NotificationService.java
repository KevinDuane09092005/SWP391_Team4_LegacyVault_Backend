package com.legacyvault.service;

import com.legacyvault.dto.NotificationCenterItem;
import com.legacyvault.dto.NotificationCenterListResponse;
import com.legacyvault.dto.NotificationDetailResponse;
import com.legacyvault.entity.InAppNotification;
import com.legacyvault.repository.InAppNotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class NotificationService {

    private final InAppNotificationRepository notificationRepository;

    public NotificationService(InAppNotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public NotificationCenterListResponse getNotificationList(String userId, Pageable pageable) {
        Page<InAppNotification> page = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH);

        List<NotificationCenterItem> items = new ArrayList<>();
        if (page.isEmpty()) {
            // Provide prototype default notifications matching Screen F7-A
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

        for (InAppNotification notif : page.getContent()) {
            String timeFormatted = notif.getCreatedAt() != null ? notif.getCreatedAt().format(dtf) : "Recent";
            items.add(new NotificationCenterItem(
                    notif.getNotificationId(),
                    notif.getTitle(),
                    notif.getContent(),
                    timeFormatted,
                    notif.getBadgeLabel(),
                    Boolean.TRUE.equals(notif.getIsRead())
            ));
        }

        int unreadCount = (int) notificationRepository.countByUserIdAndIsReadFalse(userId);
        return new NotificationCenterListResponse(items, unreadCount);
    }

    @Transactional
    public void markAllAsRead(String userId) {
        List<InAppNotification> unreadList = notificationRepository.findByUserIdAndIsReadFalse(userId);
        for (InAppNotification notif : unreadList) {
            notif.setIsRead(true);
            notificationRepository.save(notif);
        }
    }

    @Transactional(readOnly = true)
    public NotificationDetailResponse getNotificationDetail(String notificationId) {
        Optional<InAppNotification> opt = notificationRepository.findById(notificationId);
        String title = "Legal Verification Approved";
        String badge = "Approved";
        String actionDetails = "The legal verification for Case #LV-1042 has been reviewed and approved by the legal verification team. All documentation has been verified and the case has been moved to Release & Delivery stage for processing.";

        if (opt.isPresent()) {
            InAppNotification notif = opt.get();
            title = notif.getTitle();
            badge = notif.getBadgeLabel();
            actionDetails = notif.getContent();
        }

        NotificationDetailResponse.EventMetadata metadata = new NotificationDetailResponse.EventMetadata(
                "Case #LV-1042",
                "Thanh Pham",
                "24 Sep 2026, 11:45 AM",
                "Legal Verification",
                "Attorney Notary"
        );

        List<NotificationDetailResponse.TimelineItem> timeline = Arrays.asList(
                new NotificationDetailResponse.TimelineItem("Legal Verification Approved",
                        "Verification request submitted with death certificate document reviewed & approved", "24 Sep 2026, 11:45 AM"),
                new NotificationDetailResponse.TimelineItem("Document Review In Progress",
                        "Legal verifier assigned and began examining submitted death certificate", "24 Sep 2026, 10:15 AM"),
                new NotificationDetailResponse.TimelineItem("Handover Case Triggered",
                        "Dead Man's Switch timer expired without check-in response", "23 Sep 2026, 08:00 AM")
        );

        return new NotificationDetailResponse(notificationId, title, badge, actionDetails, metadata, timeline);
    }

    @Transactional
    public InAppNotification createNotification(String userId, String title, String content, String eventType, String badgeLabel, String deepLink) {
        InAppNotification notif = new InAppNotification(userId, title, content, eventType, badgeLabel);
        notif.setDeepLink(deepLink);
        return notificationRepository.save(notif);
    }
}
