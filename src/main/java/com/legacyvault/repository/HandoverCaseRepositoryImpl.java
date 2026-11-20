package com.legacyvault.repository;

import com.legacyvault.entity.HandoverCase;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HandoverCaseRepositoryImpl implements HandoverCaseRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<HandoverCase> searchVerificationQueue(String status, String search, Pageable pageable) {
        return CriteriaQuerySupport.findPage(entityManager, HandoverCase.class, (root, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("status")), status.trim().toLowerCase(Locale.ROOT)));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.like(cb.lower(root.get("caseNumber")), pattern));
            }
            return predicates;
        }, pageable);
    }
}
