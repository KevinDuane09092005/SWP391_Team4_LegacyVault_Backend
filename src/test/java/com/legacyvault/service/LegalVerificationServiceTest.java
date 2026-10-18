package com.legacyvault.service;

import com.legacyvault.dto.Actor;
import com.legacyvault.dto.DecisionOutcome;
import com.legacyvault.dto.DecisionRequest;
import com.legacyvault.entity.AssetAssignment;
import com.legacyvault.entity.CaseStatus;
import com.legacyvault.entity.Contact;
import com.legacyvault.entity.DocumentType;
import com.legacyvault.entity.HandoverCase;
import com.legacyvault.entity.LegalVerificationCase;
import com.legacyvault.repository.LegalVerificationCaseRepository;
import com.legacyvault.repository.LegalVerificationDocumentRepository;
import com.legacyvault.repository.LegalVerificationEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
        LegalVerificationCase verificationCase = newCase(caseId, executorId);
        when(caseRepository.findById(caseId)).thenReturn(Optional.of(verificationCase));
        when(documentRepository.existsByLegalVerificationCase_CaseIdAndDocumentType(
                caseId, DocumentType.DEATH_CERTIFICATE)).thenReturn(false);

        Flow5Exception error = assertThrows(Flow5Exception.class,
                () -> service.submit(new Actor(executorId, false), caseId));

        assertEquals(422, error.getStatus().value());
        assertEquals("REQUIRED_EVIDENCE_MISSING", error.getCode());
        assertEquals(CaseStatus.DRAFT, verificationCase.getStatus());
        verify(caseRepository, never()).save(verificationCase);
        verify(eventRepository, never()).save(any());
    }

    @Test
    void executorCannotSubmitAnotherExecutorsCase() {
        String caseId = UUID.randomUUID().toString();
        LegalVerificationCase verificationCase = newCase(caseId, UUID.randomUUID().toString());
        when(caseRepository.findById(caseId)).thenReturn(Optional.of(verificationCase));

        Flow5Exception error = assertThrows(Flow5Exception.class,
                () -> service.submit(new Actor(UUID.randomUUID().toString(), false), caseId));

        assertEquals(403, error.getStatus().value());
        verify(documentRepository, never()).existsByLegalVerificationCase_CaseIdAndDocumentType(
                caseId, DocumentType.DEATH_CERTIFICATE);
        verify(caseRepository, never()).save(verificationCase);
    }

    @Test
    void unassignedVerifierCannotDecideCase() {
        String caseId = UUID.randomUUID().toString();
        LegalVerificationCase verificationCase = newCase(caseId, UUID.randomUUID().toString());
        verificationCase.setStatus(CaseStatus.IN_REVIEW);
        when(caseRepository.findById(caseId)).thenReturn(Optional.of(verificationCase));

        Flow5Exception error = assertThrows(Flow5Exception.class,
                () -> service.decide(new Actor(UUID.randomUUID().toString(), true), caseId,
                        new DecisionRequest(DecisionOutcome.APPROVED, "Reviewed")));

        assertEquals(403, error.getStatus().value());
        assertEquals(CaseStatus.IN_REVIEW, verificationCase.getStatus());
        verify(caseRepository, never()).save(verificationCase);
        verify(eventRepository, never()).save(any());
    }

    @Test
    void verifierCannotViewAnotherCaseDraft() {
        String caseId = UUID.randomUUID().toString();
        LegalVerificationCase verificationCase = newCase(caseId, UUID.randomUUID().toString());
        when(caseRepository.findById(caseId)).thenReturn(Optional.of(verificationCase));

        Flow5Exception error = assertThrows(Flow5Exception.class,
                () -> service.getCase(new Actor(UUID.randomUUID().toString(), true), caseId));

        assertEquals(403, error.getStatus().value());
    }

    private LegalVerificationCase newCase(String caseId, String executorId) {
        String ownerId = UUID.randomUUID().toString();
        HandoverCase handoverCase = new HandoverCase("HC-TEST", ownerId, UUID.randomUUID().toString());
        handoverCase.setCaseId(UUID.randomUUID().toString());

        AssetAssignment assignment = new AssetAssignment(
                UUID.randomUUID().toString(), UUID.randomUUID().toString(), null);
        assignment.setAssignmentId(UUID.randomUUID().toString());

        Contact beneficiary = new Contact(ownerId, "Beneficiary", "Family", "beneficiary@example.com",
                null, true, false, "verified");
        beneficiary.setContactId(UUID.randomUUID().toString());

        return new LegalVerificationCase(caseId, handoverCase, assignment, beneficiary, executorId);
    }
}
