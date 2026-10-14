package com.legacyvault.repository;

import com.legacyvault.entity.HandoverCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HandoverCaseRepository extends JpaRepository<HandoverCase, String> {

    List<HandoverCase> findByOwnerIdOrderByTriggeredAtDesc(String ownerId);

    Optional<HandoverCase> findByCaseNumber(String caseNumber);

    boolean existsByDmsIdAndStatus(String dmsId, String status);
}
