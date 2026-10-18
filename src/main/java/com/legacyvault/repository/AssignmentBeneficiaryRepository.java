package com.legacyvault.repository;

import com.legacyvault.entity.AssignmentBeneficiary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssignmentBeneficiaryRepository extends JpaRepository<AssignmentBeneficiary, String> {

    List<AssignmentBeneficiary> findByAssignmentId(String assignmentId);

    List<AssignmentBeneficiary> findByBeneficiaryContactId(String beneficiaryContactId);

    Optional<AssignmentBeneficiary> findByAssignmentIdAndBeneficiaryContactId(String assignmentId, String beneficiaryContactId);

    void deleteByAssignmentId(String assignmentId);
}
