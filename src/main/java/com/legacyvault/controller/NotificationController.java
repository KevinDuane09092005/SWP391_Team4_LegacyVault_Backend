package com.legacyvault.controller;

import com.legacyvault.dto.NotificationCenterListResponse;
import com.legacyvault.dto.NotificationDetailResponse;
import com.legacyvault.service.NotificationService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<?> getNotificationCenterList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        try {
            String userId = authentication != null ? authentication.getName() : "user-id-default";
            NotificationCenterListResponse response = notificationService.getNotificationList(userId, PageRequest.of(page, size));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/mark-all-read")
    public ResponseEntity<?> markAllAsRead(Authentication authentication) {
        try {
            String userId = authentication != null ? authentication.getName() : "user-id-default";
            notificationService.markAllAsRead(userId);
            return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{notificationId}")
    public ResponseEntity<?> getNotificationDetail(@PathVariable String notificationId) {
        try {
            NotificationDetailResponse response = notificationService.getNotificationDetail(notificationId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
