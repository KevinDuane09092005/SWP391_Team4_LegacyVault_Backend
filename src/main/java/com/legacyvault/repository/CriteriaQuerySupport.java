package com.legacyvault.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

final class CriteriaQuerySupport {

    private CriteriaQuerySupport() {
    }

    static <T> List<T> findList(
            EntityManager entityManager,
            Class<T> entityClass,
            BiFunction<Root<T>, CriteriaBuilder, List<Predicate>> predicateBuilder,
            Sort sort
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = cb.createQuery(entityClass);
        Root<T> root = query.from(entityClass);
        List<Predicate> predicates = predicateBuilder.apply(root, cb);
        if (!predicates.isEmpty()) {
            query.where(predicates.toArray(Predicate[]::new));
        }
        applySort(cb, query, root, sort);
        return entityManager.createQuery(query).getResultList();
    }

    static <T> Page<T> findPage(
            EntityManager entityManager,
            Class<T> entityClass,
            BiFunction<Root<T>, CriteriaBuilder, List<Predicate>> predicateBuilder,
            Pageable pageable
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = cb.createQuery(entityClass);
        Root<T> root = query.from(entityClass);
        List<Predicate> predicates = predicateBuilder.apply(root, cb);
        if (!predicates.isEmpty()) {
            query.where(predicates.toArray(Predicate[]::new));
        }
        if (pageable != null) {
            applySort(cb, query, root, pageable.getSort());
        }

        TypedQuery<T> typedQuery = entityManager.createQuery(query);
        if (pageable != null && pageable.isPaged()) {
            typedQuery.setFirstResult((int) pageable.getOffset());
            typedQuery.setMaxResults(pageable.getPageSize());
        }
        List<T> content = typedQuery.getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<T> countRoot = countQuery.from(entityClass);
        countQuery.select(cb.count(countRoot));
        List<Predicate> countPredicates = predicateBuilder.apply(countRoot, cb);
        if (!countPredicates.isEmpty()) {
            countQuery.where(countPredicates.toArray(Predicate[]::new));
        }
        long total = entityManager.createQuery(countQuery).getSingleResult();

        return pageable == null ? new PageImpl<>(content) : new PageImpl<>(content, pageable, total);
    }

    private static <T> void applySort(CriteriaBuilder cb, CriteriaQuery<T> query, Root<T> root, Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return;
        }
        List<Order> orders = new ArrayList<>();
        for (Sort.Order order : sort) {
            orders.add(order.isAscending()
                    ? cb.asc(root.get(order.getProperty()))
                    : cb.desc(root.get(order.getProperty())));
        }
        if (!orders.isEmpty()) {
            query.orderBy(orders);
        }
    }
}
