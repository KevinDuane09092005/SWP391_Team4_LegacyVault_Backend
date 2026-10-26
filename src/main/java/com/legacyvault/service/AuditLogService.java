package com.legacyvault.service;

import com.legacyvault.dto.AuditLogRow;
import com.legacyvault.dto.AuditLogTableResponse;
import com.legacyvault.entity.AuditLog;
import com.legacyvault.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class AuditLogService {

    private static final String EMPTY_HASH = "0".repeat(64);
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale.ENGLISH);
    private static final DateTimeFormatter CSV_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final int MAX_PAGE_SIZE = 200;

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
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    @Transactional
    public AuditLog recordLog(String userId, String userName, String userRole, String action,
                              String target, String status, String ipAddress, String details) {
        Optional<AuditLog> latest = auditLogRepository.findTopByOrderByCreatedAtDesc();
        String previousHash = latest.map(AuditLog::getCurrentHashSha256).orElse(EMPTY_HASH);
        String hashInput = String.join(":",
                previousHash,
                safe(userId), safe(userRole), safe(action), safe(target), safe(status),
                LocalDateTime.now().toString());
        String currentHash = sha256Hex(hashInput);

        AuditLog log = new AuditLog(userId, userName, userRole, action, target, status, ipAddress);
        log.setDetails(details);
        log.setPreviousHashSha256(previousHash);
        log.setCurrentHashSha256(currentHash);
        return auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public AuditLogTableResponse searchLogs(String search, String role, String action,
                                            String fromDate, String toDate, int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Page must be non-negative and size must be between 1 and " + MAX_PAGE_SIZE);
        }
        LocalDateTime from = parseDate(fromDate, false);
        LocalDateTime to = parseDate(toDate, true);
        validateDateRange(from, to);

        String query = blankToNull(search);
        String roleFilter = blankToAll(role);
        String actionFilter = blankToAll(action);
        Page<AuditLog> resultPage = auditLogRepository.searchAuditLogs(
                query, roleFilter, actionFilter, from, to, PageRequest.of(page, size));

        List<AuditLogRow> rows = new ArrayList<>();
        for (AuditLog log : resultPage.getContent()) {
            rows.add(new AuditLogRow(
                    log.getCreatedAt() == null ? "—" : log.getCreatedAt().format(DISPLAY_TIME),
                    log.getUserName() == null ? "—" : log.getUserName(),
                    log.getAction(),
                    log.getTarget(),
                    log.getStatus(),
                    log.getIpAddress() == null ? "—" : log.getIpAddress()));
        }

        long total = resultPage.getTotalElements();
        int totalPages = resultPage.getTotalPages();
        long start = total == 0 || (long) page * size >= total ? 0 : (long) page * size + 1;
        long end = start == 0 ? 0 : Math.min((long) (page + 1) * size, total);
        String displayRange = start == 0
                ? "Showing 0 of " + total + " audit logs"
                : "Showing " + start + "-" + end + " of " + total + " audit logs";
        return new AuditLogTableResponse(rows,
                new AuditLogTableResponse.PaginationInfo(total, page + 1, totalPages, displayRange));
    }

    @Transactional(readOnly = true)
    public byte[] exportCsv(String fromDate, String toDate) {
        LocalDateTime from = parseDate(fromDate, false);
        LocalDateTime to = parseDate(toDate, true);
        validateDateRange(from, to);

        StringBuilder csv = new StringBuilder("TIME,USER,ACTION,TARGET,STATUS,IP_ADDRESS\r\n");
        List<AuditLog> logs = auditLogRepository.findForExport(from, to);
        for (AuditLog log : logs) {
            csv.append(csvCell(log.getCreatedAt() == null ? "" : log.getCreatedAt().format(CSV_TIME))).append(',')
                    .append(csvCell(log.getUserName())).append(',')
                    .append(csvCell(log.getAction())).append(',')
                    .append(csvCell(log.getTarget())).append(',')
                    .append(csvCell(log.getStatus())).append(',')
                    .append(csvCell(log.getIpAddress())).append("\r\n");
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private LocalDateTime parseDate(String value, boolean endOfDay) {
        if (value == null || value.isBlank()) return null;
        try {
            LocalDate date = LocalDate.parse(value.trim());
            return endOfDay ? date.plusDays(1).atStartOfDay().minusNanos(1) : date.atStartOfDay();
        } catch (DateTimeParseException | ArithmeticException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Dates must use YYYY-MM-DD format", e);
        }
    }

    private void validateDateRange(LocalDateTime from, LocalDateTime to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "from_date must be on or before to_date");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String blankToAll(String value) {
        String normalized = blankToNull(value);
        return normalized == null ? "ALL" : normalized;
    }

    private String csvCell(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        return escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n") || escaped.contains("\r")
                ? "\"" + escaped + "\""
                : escaped;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
