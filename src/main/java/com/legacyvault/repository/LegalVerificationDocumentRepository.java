package com.legacyvault.repository;

import com.legacyvault.entity.LegalVerificationDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LegalVerificationDocumentRepository extends JpaRepository<LegalVerificationDocument, String> {
    List<LegalVerificationDocument> findByLegalVerificationCase_CaseIdOrderByUploadedAtAsc(String caseId);
    Optional<LegalVerificationDocument> findByDocumentIdAndLegalVerificationCase_CaseId(String documentId, String caseId);
    boolean existsByLegalVerificationCase_CaseIdAndDocumentType(String caseId, com.legacyvault.entity.DocumentType documentType);
}
