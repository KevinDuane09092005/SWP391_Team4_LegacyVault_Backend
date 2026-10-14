package com.legacyvault.service;

import com.legacyvault.model.dto.Actor;
import com.legacyvault.model.dto.DecisionOutcome;
import com.legacyvault.model.dto.DecisionRequest;
import com.legacyvault.model.entity.CaseStatus;
import com.legacyvault.model.entity.DocumentType;
import com.legacyvault.model.entity.LegalVerificationCase;
import com.legacyvault.repository.LegalVerificationCaseRepository;
import com.legacyvault.repository.LegalVerificationDocumentRepository;
import com.legacyvault.repository.LegalVerificationEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LegalVerificationServiceTest {

    private final LegalVerificationCaseRepository caseRepository = mock(LegalVerificationCaseRepository.class);
    private final LegalVerificationDocumentRepository documentRepository = mock(LegalVerificationDocumentRepository.class);
    private final LegalVerificationEventRepository eventRepository = mock(LegalVerificationEventRepository.class);
    private final ActivationGateway activationGateway = mock(ActivationGateway.class);
    private final DocumentStorage documentStorage = mock(DocumentStorage.class);

    private LegalVerificationService service;

    @BeforeEach
    void setUp() {
        service = new LegalVerificationService(caseRepository, documentRepository, eventRepository,
                activationGateway, documentStorage);
    }

    @Test
    void executorCannotSubmitWithoutDeathCertificate() {
        String caseId = UUID.randomUUID().toString();
        String executorId = UUID.randomUUID().toString();
        LegalVerificationCase verificationCase = new LegalVerificationCase(
                caseId, UUID.randomUUID().toString(), executorId);
        when(caseRepository.findById(caseId)).thenReturn(Optional.of(verificationCase));
        when(documentRepository.existsByLegalVerificationCase_CaseIdAndDocumentType(
                caseId, DocumentType.DEATH_CERTIFICATE)).thenReturn(false);

        Flow5Exception error = assertThrows(Flow5Exception.class,
                () -> service.submit(new Actor(executorId, true, false), caseId));

        assertEquals(422, error.getStatus().value());
        assertEquals("REQUIRED_EVIDENCE_MISSING", error.getCode());
        assertEquals(CaseStatus.DRAFT, verificationCase.getStatus());
        verify(caseRepository, never()).save(verificationCase);
        verify(eventRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void executorCannotSubmitAnotherExecutorsCase() {
        String caseId = UUID.randomUUID().toString();
        LegalVerificationCase verificationCase = new LegalVerificationCase(
                caseId, UUID.randomUUID().toString(), UUID.randomUUID().toString());
        when(caseRepository.findById(caseId)).thenReturn(Optional.of(verificationCase));

        Flow5Exception error = assertThrows(Flow5Exception.class,
                () -> service.submit(new Actor(UUID.randomUUID().toString(), true, false), caseId));

        assertEquals(403, error.getStatus().value());
        verify(documentRepository, never()).existsByLegalVerificationCase_CaseIdAndDocumentType(
                caseId, DocumentType.DEATH_CERTIFICATE);
        verify(caseRepository, never()).save(verificationCase);
    }

    @Test
    void unassignedVerifierCannotDecideCase() {
        String caseId = UUID.randomUUID().toString();
        LegalVerificationCase verificationCase = new LegalVerificationCase(
                caseId, UUID.randomUUID().toString(), UUID.randomUUID().toString());
        verificationCase.setStatus(CaseStatus.IN_REVIEW);
        when(caseRepository.findById(caseId)).thenReturn(Optional.of(verificationCase));

        Flow5Exception error = assertThrows(Flow5Exception.class,
                () -> service.decide(new Actor(UUID.randomUUID().toString(), false, true), caseId,
                        new DecisionRequest(DecisionOutcome.APPROVED, "Reviewed")));

        assertEquals(403, error.getStatus().value());
        assertEquals(CaseStatus.IN_REVIEW, verificationCase.getStatus());
        verify(caseRepository, never()).save(verificationCase);
        verify(eventRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void verifierCannotViewAnotherCaseDraft() {
        String caseId = UUID.randomUUID().toString();
        LegalVerificationCase verificationCase = new LegalVerificationCase(
                caseId, UUID.randomUUID().toString(), UUID.randomUUID().toString());
        when(caseRepository.findById(caseId)).thenReturn(Optional.of(verificationCase));

        Flow5Exception error = assertThrows(Flow5Exception.class,
                () -> service.getCase(new Actor(UUID.randomUUID().toString(), false, true), caseId));

        assertEquals(403, error.getStatus().value());
    }
}
