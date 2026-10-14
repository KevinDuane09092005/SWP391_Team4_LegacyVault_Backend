package com.legacyvault.controller;

import com.legacyvault.dto.CheckInLogDto;
import com.legacyvault.dto.CheckInResponse;
import com.legacyvault.dto.DmsConfigRequest;
import com.legacyvault.dto.DmsStatusResponse;
import com.legacyvault.service.DmsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dms")
public class DmsController {

    private final DmsService dmsService;

    public DmsController(DmsService dmsService) {
        this.dmsService = dmsService;
    }

    @GetMapping("/status")
    public ResponseEntity<?> getDmsStatus(Authentication authentication) {
        try {
            String ownerId = getAuthenticatedUserId(authentication);
            DmsStatusResponse response = dmsService.getDmsStatus(ownerId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/config")
    public ResponseEntity<?> saveOrUpdateConfig(
            Authentication authentication,
            @Valid @RequestBody DmsConfigRequest request
    ) {
        try {
            String ownerId = getAuthenticatedUserId(authentication);
            DmsStatusResponse response = dmsService.saveOrUpdateConfig(ownerId, request);
            return ResponseEntity.ok(Map.of(
                    "message", "Dead Man's Switch configuration saved successfully",
                    "config", response
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/check-in")
    public ResponseEntity<?> checkIn(Authentication authentication) {
        try {
            String ownerId = getAuthenticatedUserId(authentication);
            CheckInResponse response = dmsService.checkIn(ownerId, "MANUAL_DASHBOARD");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/emergency-check-in")
    public ResponseEntity<?> emergencyCheckIn(Authentication authentication) {
        try {
            String ownerId = getAuthenticatedUserId(authentication);
            CheckInResponse response = dmsService.emergencyCheckIn(ownerId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<?> getAuditLogs(Authentication authentication) {
        try {
            String ownerId = getAuthenticatedUserId(authentication);
            List<CheckInLogDto> logs = dmsService.getAuditLogs(ownerId);
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private String getAuthenticatedUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("Unauthorized: No user session found");
        }
        return authentication.getName();
    }
}
