package com.legacyvault.controller;

import com.legacyvault.service.AdminSettingsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminSettingsController {

    private final AdminSettingsService adminSettingsService;

    public AdminSettingsController(AdminSettingsService adminSettingsService) {
        this.adminSettingsService = adminSettingsService;
    }

    // 5. Roles & Permissions Matrix
    @GetMapping("/permissions")
    public ResponseEntity<Map<String, Object>> getPermissions() {
        return ResponseEntity.ok(adminSettingsService.getPermissions());
    }

    @PatchMapping("/permissions")
    public ResponseEntity<Map<String, List<String>>> updatePermission(
            @RequestBody Map<String, Object> request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        String adminId = authentication != null ? authentication.getName() : null;
        String role = request.get("role") != null ? String.valueOf(request.get("role")) : null;
        String permission = request.get("permission") != null ? String.valueOf(request.get("permission")) : null;
        Boolean granted = request.get("granted") instanceof Boolean b
                ? b
                : (request.get("granted") != null ? Boolean.valueOf(String.valueOf(request.get("granted"))) : null);
        return ResponseEntity.ok(
                adminSettingsService.updatePermission(adminId, role, permission, granted, httpRequest.getRemoteAddr()));
    }

    // 6. Encryption, Backup & DMS Policies
    @GetMapping("/policies")
    public ResponseEntity<Map<String, Object>> getPolicies() {
        return ResponseEntity.ok(adminSettingsService.getPolicies());
    }

    @PutMapping("/policies")
    public ResponseEntity<Map<String, Object>> savePolicies(
            @RequestBody Map<String, Object> request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        String adminId = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(adminSettingsService.savePolicies(adminId, request, httpRequest.getRemoteAddr()));
    }

    // 7. Third-Party Integrations
    @GetMapping("/integrations")
    public ResponseEntity<List<Map<String, Object>>> listIntegrations() {
        return ResponseEntity.ok(adminSettingsService.listIntegrations());
    }

    @PatchMapping("/integrations/{id}")
    public ResponseEntity<List<Map<String, Object>>> updateIntegration(
            @PathVariable String id,
            @RequestBody Map<String, Object> request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        String adminId = authentication != null ? authentication.getName() : null;
        Boolean enabled = request.get("enabled") instanceof Boolean b
                ? b
                : (request.get("enabled") != null ? Boolean.valueOf(String.valueOf(request.get("enabled"))) : null);
        return ResponseEntity.ok(adminSettingsService.updateIntegration(adminId, id, enabled, httpRequest.getRemoteAddr()));
    }
}

