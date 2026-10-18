package com.legacyvault.repository;

import com.legacyvault.entity.VerificationDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VerificationDocumentRepository extends JpaRepository<VerificationDocument, String> {

    List<VerificationDocument> findByCaseIdOrderByUploadedAtDesc(String caseId);

    boolean existsByCaseIdAndDocumentType(String caseId, String documentType);

    long countByCaseId(String caseId);
}
