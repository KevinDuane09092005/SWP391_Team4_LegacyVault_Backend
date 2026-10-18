package com.legacyvault.repository;

import com.legacyvault.entity.VerificationReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VerificationReviewRepository extends JpaRepository<VerificationReview, String> {

    List<VerificationReview> findByCaseIdOrderByReviewedAtDesc(String caseId);
}
