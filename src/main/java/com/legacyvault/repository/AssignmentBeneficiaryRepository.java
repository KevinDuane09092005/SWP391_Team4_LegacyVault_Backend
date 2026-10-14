package com.legacyvault.repository;

import com.legacyvault.entity.AssignmentBeneficiary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssignmentBeneficiaryRepository extends JpaRepository<AssignmentBeneficiary, String> {

    List<AssignmentBeneficiary> findByAssignmentId(String assignmentId);

    void deleteByAssignmentId(String assignmentId);
}
