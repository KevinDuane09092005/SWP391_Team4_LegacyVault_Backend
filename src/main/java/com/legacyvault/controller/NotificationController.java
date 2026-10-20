package com.legacyvault.controller;

import com.legacyvault.dto.NotificationCenterListResponse;
import com.legacyvault.dto.NotificationDetailResponse;
import com.legacyvault.service.BillingException;
import com.legacyvault.service.NotificationService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public NotificationCenterListResponse getNotificationCenterList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        if (page < 0 || size < 1 || size > 100) {
            throw BillingException.badRequest("INVALID_PAGE", "Page must be non-negative and size must be between 1 and 100.");
        }
        return notificationService.getNotificationList(userId(authentication), PageRequest.of(page, size));
    }

    @PostMapping("/mark-all-read")
    public ResponseEntity<Map<String, String>> markAllAsRead(Authentication authentication) {
        notificationService.markAllAsRead(userId(authentication));
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }

    @GetMapping("/{notificationId}")
    public NotificationDetailResponse getNotificationDetail(
            @PathVariable String notificationId,
            Authentication authentication) {
        return notificationService.getNotificationDetail(userId(authentication), notificationId);
    }

    private String userId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) {
            throw BillingException.unauthorized();
        }
        return authentication.getName();
    }
}
