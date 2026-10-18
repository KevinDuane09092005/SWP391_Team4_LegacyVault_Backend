package com.legacyvault.repository;

import com.legacyvault.entity.BeneficiaryClaimSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BeneficiaryClaimSessionRepository extends JpaRepository<BeneficiaryClaimSession, String> {

    Optional<BeneficiaryClaimSession> findByCaseIdAndBeneficiaryContactId(String caseId, String beneficiaryContactId);

    Optional<BeneficiaryClaimSession> findByClaimTokenHash(String claimTokenHash);

    Optional<BeneficiaryClaimSession> findByAccessCodeHash(String accessCodeHash);

    List<BeneficiaryClaimSession> findByCaseId(String caseId);
}
