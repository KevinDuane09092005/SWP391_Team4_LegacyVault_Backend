package com.legacyvault.repository;

import com.legacyvault.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, String>, AuditLogRepositoryCustom {

    Optional<AuditLog> findTopByOrderByCreatedAtDesc();

    default Page<AuditLog> searchAuditLogs(
            String search,
            String role,
            String action,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable
    ) {
        return searchAuditLogs(search, role, action, "ALL", fromDate, toDate, pageable);
    }

    List<AuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime from, LocalDateTime to);

    default List<AuditLog> findLogsForExport(LocalDateTime fromDate, LocalDateTime toDate) {
        return findLogsForExportFiltered(null, "ALL", "ALL", "ALL", fromDate, toDate);
    }
}
