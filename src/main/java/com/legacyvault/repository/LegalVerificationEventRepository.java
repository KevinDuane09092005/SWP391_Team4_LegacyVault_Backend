package com.legacyvault.repository;

import com.legacyvault.model.entity.LegalVerificationEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LegalVerificationEventRepository extends JpaRepository<LegalVerificationEvent, String> {
    List<LegalVerificationEvent> findByLegalVerificationCase_CaseIdOrderByOccurredAtAsc(String caseId);
}
