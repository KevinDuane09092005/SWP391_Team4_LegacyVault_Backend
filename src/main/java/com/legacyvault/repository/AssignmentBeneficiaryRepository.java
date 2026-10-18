package com.legacyvault.repository;

import com.legacyvault.entity.AssignmentBeneficiary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssignmentBeneficiaryRepository extends JpaRepository<AssignmentBeneficiary, String> {

    List<AssignmentBeneficiary> findByAssignmentId(String assignmentId);

    boolean existsByAssignmentIdAndBeneficiaryContactId(String assignmentId, String beneficiaryContactId);

    void deleteByAssignmentId(String assignmentId);
}
