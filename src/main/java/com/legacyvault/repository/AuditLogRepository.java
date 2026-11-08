package com.legacyvault.repository;

import com.legacyvault.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, String> {

    Optional<AuditLog> findTopByOrderByCreatedAtDesc();

    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:role IS NULL OR :role = 'ALL' OR UPPER(a.userRole) = UPPER(:role) " +
           "OR (UPPER(:role) IN ('ADMIN', 'SYSTEM_ADMIN') AND UPPER(a.userRole) IN ('ADMIN', 'SYSTEM_ADMIN')) " +
           "OR (UPPER(:role) IN ('EXECUTOR', 'DIGITAL_EXECUTOR') AND UPPER(a.userRole) IN ('EXECUTOR', 'DIGITAL_EXECUTOR'))) AND " +
           "(:action IS NULL OR :action = 'ALL' OR a.action = :action) AND " +
           "(:status IS NULL OR :status = 'ALL' OR UPPER(a.status) = UPPER(:status)) AND " +
           "(:search IS NULL OR LOWER(a.target) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(a.userName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(a.action) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(COALESCE(a.details, '')) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:fromDate IS NULL OR a.createdAt >= :fromDate) AND " +
           "(:toDate IS NULL OR a.createdAt <= :toDate) " +
           "ORDER BY a.createdAt DESC")
    Page<AuditLog> searchAuditLogs(
            @Param("search") String search,
            @Param("role") String role,
            @Param("action") String action,
            @Param("status") String status,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );

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

    @Query("SELECT a FROM AuditLog a WHERE " +
            "(:fromDate IS NULL OR a.createdAt >= :fromDate) AND " +
            "(:toDate IS NULL OR a.createdAt <= :toDate) " +
            "ORDER BY a.createdAt DESC")
    List<AuditLog> findLogsForExport(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate);

    @Query("SELECT a FROM AuditLog a WHERE " +
            "(:role IS NULL OR :role = 'ALL' OR UPPER(a.userRole) = UPPER(:role) " +
            "OR (UPPER(:role) IN ('ADMIN', 'SYSTEM_ADMIN') AND UPPER(a.userRole) IN ('ADMIN', 'SYSTEM_ADMIN')) " +
            "OR (UPPER(:role) IN ('EXECUTOR', 'DIGITAL_EXECUTOR') AND UPPER(a.userRole) IN ('EXECUTOR', 'DIGITAL_EXECUTOR'))) AND " +
            "(:action IS NULL OR :action = 'ALL' OR a.action = :action) AND " +
            "(:status IS NULL OR :status = 'ALL' OR UPPER(a.status) = UPPER(:status)) AND " +
            "(:search IS NULL OR LOWER(a.target) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(a.userName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(a.action) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(COALESCE(a.details, '')) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
            "(:fromDate IS NULL OR a.createdAt >= :fromDate) AND " +
            "(:toDate IS NULL OR a.createdAt <= :toDate) " +
            "ORDER BY a.createdAt DESC")
    List<AuditLog> findLogsForExportFiltered(
            @Param("search") String search,
            @Param("role") String role,
            @Param("action") String action,
            @Param("status") String status,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate);
}
