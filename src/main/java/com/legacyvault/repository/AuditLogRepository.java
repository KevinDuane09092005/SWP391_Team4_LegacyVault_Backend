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
           "(:role IS NULL OR :role = 'ALL' OR a.userRole = :role) AND " +
           "(:action IS NULL OR :action = 'ALL' OR a.action = :action) AND " +
           "(:search IS NULL OR LOWER(a.target) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(a.userName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(a.action) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:fromDate IS NULL OR a.createdAt >= :fromDate) AND " +
           "(:toDate IS NULL OR a.createdAt <= :toDate) " +
           "ORDER BY a.createdAt DESC")
    Page<AuditLog> searchAuditLogs(
            @Param("search") String search,
            @Param("role") String role,
            @Param("action") String action,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );

    List<AuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime from, LocalDateTime to);
}
