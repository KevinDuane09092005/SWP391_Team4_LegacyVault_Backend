package com.legacyvault.repository;

import com.legacyvault.entity.HandoverCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HandoverCaseRepository extends JpaRepository<HandoverCase, String> {

    List<HandoverCase> findByOwnerIdOrderByTriggeredAtDesc(String ownerId);

    Optional<HandoverCase> findByCaseNumber(String caseNumber);

    boolean existsByDmsIdAndStatus(String dmsId, String status);

    Page<HandoverCase> findByStatus(String status, Pageable pageable);

    Page<HandoverCase> findByStatusIn(List<String> statuses, Pageable pageable);

    long countByStatus(String status);

    long countByStatusIgnoreCase(String status);

    @Query("SELECT c FROM HandoverCase c WHERE (:status IS NULL OR c.status = :status) " +
           "AND (:search IS NULL OR LOWER(c.caseNumber) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<HandoverCase> searchVerificationQueue(@Param("status") String status,
                                                @Param("search") String search,
                                                Pageable pageable);

    List<HandoverCase> findByAssignedVerifierId(String verifierId);
}
