package com.legacyvault.repository;

import com.legacyvault.entity.BeneficiaryFaceEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BeneficiaryFaceEnrollmentRepository extends JpaRepository<BeneficiaryFaceEnrollment, String> {

    Optional<BeneficiaryFaceEnrollment> findByBeneficiaryContactId(String beneficiaryContactId);

    void deleteByBeneficiaryContactId(String beneficiaryContactId);
}
