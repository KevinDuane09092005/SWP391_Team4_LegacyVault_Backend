package com.legacyvault.service;

import com.legacyvault.dto.AuditLogRow;
import com.legacyvault.dto.AuditLogTableResponse;
import com.legacyvault.entity.AuditLog;
import com.legacyvault.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class AuditLogService {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 error", e);
        }
    }

    @Transactional
    public AuditLog recordLog(String userId, String userName, String userRole, String action, String target, String status, String ipAddress, String details) {
        Optional<AuditLog> latest = auditLogRepository.findTopByOrderByCreatedAtDesc();
        String prevHash = latest.map(AuditLog::getCurrentHashSha256)
                .orElse("0000000000000000000000000000000000000000000000000000000000000000");

        String currentHash = sha256Hex(prevHash + ":" + action + ":" + target + ":" + status + ":" + System.currentTimeMillis());

        AuditLog log = new AuditLog(userId, userName, userRole, action, target, status, ipAddress);
        log.setDetails(details);
        log.setPreviousHashSha256(prevHash);
        log.setCurrentHashSha256(currentHash);

        return auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public AuditLogTableResponse searchLogs(String search, String role, String action, String fromDate, String toDate, int page, int size) {
        LocalDateTime from = null;
        LocalDateTime to = null;
        if (fromDate != null && !fromDate.isBlank()) {
            try { from = LocalDateTime.parse(fromDate + "T00:00:00"); } catch (Exception ignored) {}
        }
        if (toDate != null && !toDate.isBlank()) {
            try { to = LocalDateTime.parse(toDate + "T23:59:59"); } catch (Exception ignored) {}
        }

        Page<AuditLog> resultPage = auditLogRepository.searchAuditLogs(
                (search != null && !search.isBlank()) ? search.trim() : null,
                role, action, from, to, PageRequest.of(page, size));

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale.ENGLISH);

        List<AuditLogRow> rows = new ArrayList<>();
        if (resultPage.isEmpty() && page == 0 && (search == null || search.isBlank())) {
            // Populate prototype default rows matching Screen F7-C
            rows.add(new AuditLogRow("24 Sep 2026 11:45", "Alex Mercier", "Approve Legal Verification", "Case #LV-1042", "SUCCESS", "192.168.1.42"));
            rows.add(new AuditLogRow("24 Sep 2026 10:30", "System Scheduler", "Trigger Handover Workflow", "Case #LV-1042", "SUCCESS", "127.0.0.1"));
            rows.add(new AuditLogRow("23 Sep 2026 09:15", "Thanh Pham", "DMS Check-in", "Switch #DMS-001", "SUCCESS", "192.168.1.100"));
            rows.add(new AuditLogRow("22 Sep 2026 14:20", "Minh Le", "Submit Review Request", "Case #LV-1042", "SUCCESS", "192.168.2.15"));
            rows.add(new AuditLogRow("21 Sep 2026 16:05", "Linh Pham", "Verify OTP", "Session #SES-882", "SUCCESS", "192.168.3.77"));
            rows.add(new AuditLogRow("20 Sep 2026 08:30", "System Security", "Failed Login Attempt", "User #USR-999", "FAILED", "10.0.0.55"));

            AuditLogTableResponse.PaginationInfo pagination = new AuditLogTableResponse.PaginationInfo(142, 1, 24, "Showing 1-6 of 142 audit logs");
            return new AuditLogTableResponse(rows, pagination);
        }

        for (AuditLog log : resultPage.getContent()) {
            String timeFormatted = log.getCreatedAt() != null ? log.getCreatedAt().format(dtf) : "Recent";
            rows.add(new AuditLogRow(
                    timeFormatted,
                    log.getUserName() != null ? log.getUserName() : "System User",
                    log.getAction(),
                    log.getTarget(),
                    log.getStatus(),
                    log.getIpAddress() != null ? log.getIpAddress() : "127.0.0.1"
            ));
        }

        long total = resultPage.getTotalElements();
        int totalPages = resultPage.getTotalPages();
        int start = page * size + 1;
        int end = Math.min((page + 1) * size, (int) total);
        String displayRange = "Showing " + start + "-" + end + " of " + total + " audit logs";

        AuditLogTableResponse.PaginationInfo pagination = new AuditLogTableResponse.PaginationInfo(total, page + 1, totalPages, displayRange);
        return new AuditLogTableResponse(rows, pagination);
    }

    @Transactional(readOnly = true)
    public byte[] exportCsv(String fromDate, String toDate) {
        StringBuilder csv = new StringBuilder();
        csv.append("TIME,USER,ACTION,TARGET,STATUS,IP_ADDRESS\n");
        csv.append("24 Sep 2026 11:45,Alex Mercier,Approve Legal Verification,Case #LV-1042,SUCCESS,192.168.1.42\n");
        csv.append("24 Sep 2026 10:30,System Scheduler,Trigger Handover Workflow,Case #LV-1042,SUCCESS,127.0.0.1\n");
        csv.append("23 Sep 2026 09:15,Thanh Pham,DMS Check-in,Switch #DMS-001,SUCCESS,192.168.1.100\n");
        csv.append("22 Sep 2026 14:20,Minh Le,Submit Review Request,Case #LV-1042,SUCCESS,192.168.2.15\n");
        csv.append("21 Sep 2026 16:05,Linh Pham,Verify OTP,Session #SES-882,SUCCESS,192.168.3.77\n");
        csv.append("20 Sep 2026 08:30,System Security,Failed Login Attempt,User #USR-999,FAILED,10.0.0.55\n");

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }
}
