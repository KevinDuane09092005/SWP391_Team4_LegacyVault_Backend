package com.legacyvault.repository;

import com.legacyvault.entity.HandoverCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HandoverCaseRepository extends JpaRepository<HandoverCase, String>, HandoverCaseRepositoryCustom {

    List<HandoverCase> findByOwnerIdOrderByTriggeredAtDesc(String ownerId);

    Optional<HandoverCase> findByCaseNumber(String caseNumber);

    boolean existsByDmsIdAndStatus(String dmsId, String status);

    Page<HandoverCase> findByStatus(String status, Pageable pageable);

    Page<HandoverCase> findByStatusIn(List<String> statuses, Pageable pageable);

    long countByStatus(String status);

    long countByStatusIgnoreCase(String status);

    List<HandoverCase> findByAssignedVerifierId(String verifierId);
}
