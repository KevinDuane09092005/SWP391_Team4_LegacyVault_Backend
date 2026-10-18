package com.legacyvault.controller;

import com.legacyvault.dto.AdminDashboardResponse;
import com.legacyvault.dto.AuditLogTableResponse;
import com.legacyvault.service.AdminDashboardService;
import com.legacyvault.service.AuditLogService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminAuditController {

    private final AuditLogService auditLogService;
    private final AdminDashboardService adminDashboardService;

    public AdminAuditController(AuditLogService auditLogService, AdminDashboardService adminDashboardService) {
        this.auditLogService = auditLogService;
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<?> searchAuditLogs(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "ALL") String role,
            @RequestParam(defaultValue = "ALL") String action,
            @RequestParam(name = "from_date", required = false) String fromDate,
            @RequestParam(name = "to_date", required = false) String toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size) {
        try {
            AuditLogTableResponse response = auditLogService.searchLogs(search, role, action, fromDate, toDate, page, size);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping(value = "/audit-logs/export-csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportAuditLogsCsv(
            @RequestParam(name = "from_date", required = false) String fromDate,
            @RequestParam(name = "to_date", required = false) String toDate) {
        byte[] csvData = auditLogService.exportCsv(fromDate, toDate);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit-logs-export.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvData);
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> getAdminDashboard() {
        try {
            AdminDashboardResponse response = adminDashboardService.getAdminDashboard();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/access-requests/{requestId}/approve")
    public ResponseEntity<?> approveAccessRequest(@PathVariable String requestId, Authentication authentication) {
        try {
            String adminId = authentication != null ? authentication.getName() : "admin-user-id";
            adminDashboardService.approveAccessRequest(requestId, adminId);
            return ResponseEntity.ok(Map.of("message", "Access request approved", "request_id", requestId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/access-requests/{requestId}/reject")
    public ResponseEntity<?> rejectAccessRequest(@PathVariable String requestId, Authentication authentication) {
        try {
            String adminId = authentication != null ? authentication.getName() : "admin-user-id";
            adminDashboardService.rejectAccessRequest(requestId, adminId);
            return ResponseEntity.ok(Map.of("message", "Access request rejected", "request_id", requestId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
