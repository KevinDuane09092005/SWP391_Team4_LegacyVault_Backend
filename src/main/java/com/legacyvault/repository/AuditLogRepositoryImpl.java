package com.legacyvault.repository;

import com.legacyvault.entity.AuditLog;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AuditLogRepositoryImpl implements AuditLogRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<AuditLog> searchAuditLogs(
            String search,
            String role,
            String action,
            String status,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable
    ) {
        Pageable effectivePageable = (pageable == null || pageable.getSort().isUnsorted())
                ? PageRequest.of(
                        pageable != null ? pageable.getPageNumber() : 0,
                        pageable != null ? pageable.getPageSize() : 20,
                        Sort.by(Sort.Direction.DESC, "createdAt"))
                : pageable;
        return CriteriaQuerySupport.findPage(
                entityManager,
                AuditLog.class,
                (root, cb) -> buildPredicates(root, cb, search, role, action, status, fromDate, toDate),
                effectivePageable
        );
    }

    @Override
    public List<AuditLog> findLogsForExportFiltered(
            String search,
            String role,
            String action,
            String status,
            LocalDateTime fromDate,
            LocalDateTime toDate
    ) {
        return CriteriaQuerySupport.findList(
                entityManager,
                AuditLog.class,
                (root, cb) -> buildPredicates(root, cb, search, role, action, status, fromDate, toDate),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
    }

    private static List<Predicate> buildPredicates(
            Root<AuditLog> root,
            CriteriaBuilder cb,
            String search,
            String role,
            String action,
            String status,
            LocalDateTime fromDate,
            LocalDateTime toDate
    ) {
        List<Predicate> predicates = new ArrayList<>();
        if (role != null && !"ALL".equalsIgnoreCase(role)) {
            String upperRole = role.trim().toUpperCase(Locale.ROOT);
            if ("ADMIN".equals(upperRole) || "SYSTEM_ADMIN".equals(upperRole)) {
                predicates.add(cb.upper(root.get("userRole")).in(List.of("ADMIN", "SYSTEM_ADMIN")));
            } else if ("EXECUTOR".equals(upperRole) || "DIGITAL_EXECUTOR".equals(upperRole)) {
                predicates.add(cb.upper(root.get("userRole")).in(List.of("EXECUTOR", "DIGITAL_EXECUTOR")));
            } else {
                predicates.add(cb.equal(cb.upper(root.get("userRole")), upperRole));
            }
        }
        if (action != null && !"ALL".equalsIgnoreCase(action)) {
            predicates.add(cb.equal(root.get("action"), action));
        }
        if (status != null && !"ALL".equalsIgnoreCase(status)) {
            predicates.add(cb.equal(cb.upper(root.get("status")), status.trim().toUpperCase(Locale.ROOT)));
        }
        if (search != null && !search.isEmpty()) {
            String pattern = "%" + search.toLowerCase(Locale.ROOT) + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("target")), pattern),
                    cb.like(cb.lower(root.get("userName")), pattern),
                    cb.like(cb.lower(root.get("action")), pattern),
                    cb.like(cb.lower(cb.coalesce(root.get("details"), "")), pattern)
            ));
        }
        if (fromDate != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
        }
        if (toDate != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toDate));
        }
        return predicates;
    }
}
