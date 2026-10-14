package com.legacyvault.repository;

import com.legacyvault.model.entity.CaseStatus;
import com.legacyvault.model.entity.LegalVerificationCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LegalVerificationCaseRepository extends JpaRepository<LegalVerificationCase, String> {
    boolean existsByActivationId(String activationId);
    Page<LegalVerificationCase> findByExecutorUserId(String executorUserId, Pageable pageable);
    Page<LegalVerificationCase> findByExecutorUserIdAndStatus(String executorUserId, CaseStatus status, Pageable pageable);

    @Query("select c from LegalVerificationCase c " +
            "where ((c.status = :submittedStatus and c.reviewerUserId is null) " +
            "or c.reviewerUserId = :reviewerUserId) " +
            "and (:status is null or c.status = :status)")
    Page<LegalVerificationCase> findVisibleToReviewer(
            @Param("reviewerUserId") String reviewerUserId,
            @Param("submittedStatus") CaseStatus submittedStatus,
            @Param("status") CaseStatus status,
            Pageable pageable);
}
