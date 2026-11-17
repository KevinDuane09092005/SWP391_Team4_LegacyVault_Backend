package com.legacyvault.repository;

import com.legacyvault.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UserRepositoryImpl implements UserRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<User> searchAdminUsers(String search, String role, String status, Pageable pageable) {
        return CriteriaQuerySupport.findPage(entityManager, User.class, (root, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("fullName")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern)
                ));
            }
            if (role != null && !role.isBlank()) {
                String lowerRole = role.trim().toLowerCase(Locale.ROOT);
                if ("digital_executor".equals(lowerRole)) {
                    predicates.add(cb.lower(root.get("role")).in(List.of("digital_executor", "executor")));
                } else if ("admin".equals(lowerRole)) {
                    predicates.add(cb.lower(root.get("role")).in(List.of("admin", "system_admin")));
                } else {
                    predicates.add(cb.equal(cb.lower(root.get("role")), lowerRole));
                }
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(
                        cb.upper(cb.coalesce(root.get("accountStatus"), "ACTIVE")),
                        status.trim().toUpperCase(Locale.ROOT)
                ));
            }
            return predicates;
        }, pageable);
    }
}
