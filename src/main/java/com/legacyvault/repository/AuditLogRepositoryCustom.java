package com.legacyvault.repository;

import com.legacyvault.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepositoryCustom {
    Page<AuditLog> searchAuditLogs(
            String search,
            String role,
            String action,
            String status,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable
    );

    List<AuditLog> findLogsForExportFiltered(
            String search,
            String role,
            String action,
            String status,
            LocalDateTime fromDate,
            LocalDateTime toDate
    );
}
