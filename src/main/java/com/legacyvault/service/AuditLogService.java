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
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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
        if (page < 0) {
            throw new IllegalArgumentException("Page must be zero or greater.");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100.");
        }
        LocalDateTime from = parseDate(fromDate, false);
        LocalDateTime to = parseDate(toDate, true);
        if (from != null && to != null && to.isBefore(from)) {
            throw new IllegalArgumentException("The end date must be on or after the start date.");
        }

        Page<AuditLog> resultPage = auditLogRepository.searchAuditLogs(
                (search != null && !search.isBlank()) ? search.trim() : null,
                normalizeFilter(role), normalizeFilter(action), from, to, PageRequest.of(page, size));

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale.ENGLISH);

        List<AuditLogRow> rows = new ArrayList<>();

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
        int start = total == 0 ? 0 : page * size + 1;
        int end = Math.min((page + 1) * size, (int) total);
        String displayRange = "Showing " + start + "-" + end + " of " + total + " audit logs";

        AuditLogTableResponse.PaginationInfo pagination = new AuditLogTableResponse.PaginationInfo(total, page + 1, totalPages, displayRange);
        return new AuditLogTableResponse(rows, pagination);
    }

    @Transactional(readOnly = true)
    public byte[] exportCsv(String fromDate, String toDate) {
        LocalDateTime from = parseDate(fromDate, false);
        LocalDateTime to = parseDate(toDate, true);
        if (from != null && to != null && to.isBefore(from)) {
            throw new IllegalArgumentException("The end date must be on or after the start date.");
        }

        StringBuilder csv = new StringBuilder();
        csv.append("TIME,USER,ACTION,TARGET,STATUS,IP_ADDRESS\n");
        DateTimeFormatter csvTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (AuditLog log : auditLogRepository.findLogsForExport(from, to)) {
            csv.append(csvEscape(log.getCreatedAt() == null ? "" : log.getCreatedAt().format(csvTime))).append(',')
                    .append(csvEscape(log.getUserName())).append(',')
                    .append(csvEscape(log.getAction())).append(',')
                    .append(csvEscape(log.getTarget())).append(',')
                    .append(csvEscape(log.getStatus())).append(',')
                    .append(csvEscape(log.getIpAddress())).append('\n');
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private LocalDateTime parseDate(String value, boolean endOfDay) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            LocalDate date = LocalDate.parse(value.trim());
            return endOfDay ? date.atTime(23, 59, 59, 999_999_999) : date.atStartOfDay();
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Dates must use YYYY-MM-DD format.");
        }
    }

    private String normalizeFilter(String value) {
        return value == null || value.isBlank() || value.equalsIgnoreCase("ALL")
                ? "ALL" : value.trim();
    }

    private String csvEscape(String value) {
        String safe = value == null ? "" : value;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
}
