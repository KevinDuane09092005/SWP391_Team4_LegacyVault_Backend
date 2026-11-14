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
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class NotificationService {

    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH);

    private static final Pattern DEEP_LINK_CASE_PATTERN = Pattern.compile("(?:handover/|cases/|caseId=)([A-Za-z0-9_-]+)");
    private static final Pattern TEXT_CASE_PATTERN = Pattern.compile("(?:Case|case)\\s*(?:#|:\\s*#?|\\s+)\\s*([A-Z0-9]+(?:-[A-Z0-9]+)?)");

    private final InAppNotificationRepository notificationRepository;

    public NotificationService(InAppNotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public NotificationCenterListResponse getNotificationList(String userId, Pageable pageable) {
        Page<InAppNotification> page = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        List<NotificationCenterItem> items = new ArrayList<>();
        for (InAppNotification notification : page.getContent()) {
            String formattedTime = notification.getCreatedAt() == null
                    ? "Recent" : notification.getCreatedAt().format(DISPLAY_TIME);
            String caseId = extractCaseId(notification.getDeepLink(), notification.getTitle(), notification.getContent());
            NotificationCenterItem item = new NotificationCenterItem(
                    notification.getNotificationId(), notification.getTitle(), notification.getContent(),
                    formattedTime, notification.getBadgeLabel(), Boolean.TRUE.equals(notification.getIsRead()),
                    caseId, notification.getDeepLink());
            item.setEventType(notification.getEventType());
            items.add(item);
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

    @Transactional
    public void markAsRead(String userId, String notificationId) {
        InAppNotification notification = notificationRepository.findByNotificationIdAndUserId(notificationId, userId)
                .orElseThrow(BillingException::notFound);
        if (!Boolean.TRUE.equals(notification.getIsRead())) {
            notification.setIsRead(true);
            notificationRepository.save(notification);
        }
    }

    @Transactional
    public NotificationDetailResponse getNotificationDetail(String userId, String notificationId) {
        Optional<InAppNotification> ownedNotification =
                notificationRepository.findByNotificationIdAndUserId(notificationId, userId);
        if (ownedNotification.isPresent()) {
            InAppNotification notification = ownedNotification.get();
            if (!Boolean.TRUE.equals(notification.getIsRead())) {
                notification.setIsRead(true);
                notificationRepository.save(notification);
            }
            String occurredAt = notification.getCreatedAt() == null
                    ? "Recent" : notification.getCreatedAt().format(DISPLAY_TIME);
            String caseId = extractCaseId(notification.getDeepLink(), notification.getTitle(), notification.getContent());
            return new NotificationDetailResponse(
                    notificationId,
                    notification.getTitle(),
                    notification.getBadgeLabel(),
                    notification.getContent(),
                    new NotificationDetailResponse.EventMetadata(
                            caseId != null ? caseId : "Case #LV-1042", null, occurredAt, notification.getEventType(), "LegacyVault"),
                    List.of(new NotificationDetailResponse.TimelineItem(
                            notification.getTitle(), notification.getContent(), occurredAt)));
        }

        throw BillingException.notFound();
    }

    @Transactional
    public InAppNotification createNotification(String userId, String title, String content,
                                                String eventType, String badgeLabel, String deepLink) {
        InAppNotification notification = new InAppNotification(userId, title, content, eventType, badgeLabel);
        notification.setDeepLink(deepLink);
        return notificationRepository.save(notification);
    }

    public static String extractCaseId(String deepLink, String title, String content) {
        if (deepLink != null && !deepLink.isBlank()) {
            Matcher m = DEEP_LINK_CASE_PATTERN.matcher(deepLink);
            if (m.find()) {
                return m.group(1);
            }
        }
        String combined = (title != null ? title : "") + " " + (content != null ? content : "");
        Matcher m = TEXT_CASE_PATTERN.matcher(combined);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }
}
