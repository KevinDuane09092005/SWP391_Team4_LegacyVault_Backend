package com.legacyvault.service;

import com.legacyvault.dto.BeneficiaryFaceEnrollmentRequest;
import com.legacyvault.dto.BeneficiaryFaceVerificationRequest;
import com.legacyvault.entity.BeneficiaryClaimSession;
import com.legacyvault.entity.BeneficiaryFaceEnrollment;
import com.legacyvault.entity.HandoverCase;
import com.legacyvault.repository.BeneficiaryClaimSessionRepository;
import com.legacyvault.repository.BeneficiaryFaceEnrollmentRepository;
import com.legacyvault.repository.HandoverCaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BeneficiaryFaceVerificationServiceTest {

    private static final String CASE_ID = "case-uuid-1042";
    private static final String CASE_NUMBER = "LV-1042";
    private static final String BENEFICIARY_ID = "beneficiary-uuid-1";
    private static final String TEST_KEY = Base64.getEncoder().encodeToString(new byte[32]);

    @Mock
    private BeneficiaryFaceEnrollmentRepository enrollmentRepository;

    @Mock
    private BeneficiaryClaimSessionRepository claimSessionRepository;

    @Mock
    private HandoverCaseRepository handoverCaseRepository;

    private BeneficiaryFaceVerificationService service;

    @BeforeEach
    void setUp() {
        service = new BeneficiaryFaceVerificationService(
                enrollmentRepository,
                claimSessionRepository,
                handoverCaseRepository,
                new FaceEmbeddingEncryptionService(TEST_KEY));
    }

    @Test
    void enrollmentRequiresConsentAndPersistsOnlyEncryptedEmbedding() {
        BeneficiaryClaimSession claim = activeClaim();
        stubEnrollmentClaim(claim);
        when(enrollmentRepository.findByBeneficiaryContactId(BENEFICIARY_ID)).thenReturn(Optional.empty());
        when(claimSessionRepository.findByBeneficiaryContactIdAndTokenStatus(BENEFICIARY_ID, "ACTIVE"))
                .thenReturn(List.of(claim));
        List<Double> embedding = embedding(0.25);

        assertThrows(ResponseStatusException.class, () -> service.enroll(
                BENEFICIARY_ID, new BeneficiaryFaceEnrollmentRequest(false, embedding)));
        verify(enrollmentRepository, never()).save(any());

        var response = service.enroll(BENEFICIARY_ID,
                new BeneficiaryFaceEnrollmentRequest(true, embedding));

        assertTrue(response.enrolled());
        ArgumentCaptor<BeneficiaryFaceEnrollment> profileCaptor =
                ArgumentCaptor.forClass(BeneficiaryFaceEnrollment.class);
        verify(enrollmentRepository).save(profileCaptor.capture());
        BeneficiaryFaceEnrollment saved = profileCaptor.getValue();
        assertNotEquals(embedding.get(0), saved.getEncryptedEmbedding()[0]);
        assertArrayEquals(toPrimitive(embedding),
                new FaceEmbeddingEncryptionService(TEST_KEY).decrypt(saved.getEncryptedEmbedding()));
    }

    @Test
    void matchingLiveScanMarksOnlyTheSelectedClaimVerifiedAndSessionCannotReplay() {
        BeneficiaryClaimSession claim = activeClaim();
        BeneficiaryFaceEnrollment profile = enrolledProfile(0.25);
        HandoverCase handoverCase = new HandoverCase(CASE_NUMBER, "owner-uuid-1", "dms-uuid-1");
        handoverCase.setCaseId(CASE_ID);
        when(claimSessionRepository.findByCaseIdAndBeneficiaryContactId(CASE_ID, BENEFICIARY_ID))
                .thenReturn(Optional.of(claim));
        when(handoverCaseRepository.findByCaseNumber(CASE_NUMBER)).thenReturn(Optional.of(handoverCase));
        when(enrollmentRepository.findByBeneficiaryContactId(BENEFICIARY_ID)).thenReturn(Optional.of(profile));

        var session = service.createAccessSession(CASE_ID, BENEFICIARY_ID);
        var result = service.completeAccess(CASE_NUMBER, session.sessionId(), BENEFICIARY_ID,
                new BeneficiaryFaceVerificationRequest(true, 0.96, 0.71, embedding(0.25)));

        assertTrue(result.passed());
        assertTrue(result.demoOnly());
        assertEquals("VERIFIED", claim.getKycFacialStatus());
        verify(claimSessionRepository, atLeastOnce()).save(claim);
        assertThrows(ResponseStatusException.class, () -> service.completeAccess(CASE_NUMBER,
                session.sessionId(), BENEFICIARY_ID,
                new BeneficiaryFaceVerificationRequest(true, 0.96, 0.71, embedding(0.25))));
    }

    @Test
    void differentFaceOrFailedLivenessLeavesClaimUnverified() {
        BeneficiaryClaimSession claim = activeClaim();
        BeneficiaryFaceEnrollment profile = enrolledProfile(0.0);
        when(claimSessionRepository.findByCaseIdAndBeneficiaryContactId(CASE_ID, BENEFICIARY_ID))
                .thenReturn(Optional.of(claim));
        when(enrollmentRepository.findByBeneficiaryContactId(BENEFICIARY_ID)).thenReturn(Optional.of(profile));

        var session = service.createAccessSession(CASE_ID, BENEFICIARY_ID);
        var result = service.completeAccess(CASE_ID, session.sessionId(), BENEFICIARY_ID,
                new BeneficiaryFaceVerificationRequest(false, 0.55, 0.99, embedding(1.0)));

        assertFalse(result.passed());
        assertEquals("FAILED", result.status());
        assertEquals("FAILED", claim.getKycFacialStatus());
    }

    @Test
    void faceSessionCannotBeUsedForAnotherCaseOrBeneficiary() {
        BeneficiaryClaimSession claim = activeClaim();
        when(claimSessionRepository.findByCaseIdAndBeneficiaryContactId(CASE_ID, BENEFICIARY_ID))
                .thenReturn(Optional.of(claim));
        when(enrollmentRepository.findByBeneficiaryContactId(BENEFICIARY_ID))
                .thenReturn(Optional.of(enrolledProfile(0.25)));
        var session = service.createAccessSession(CASE_ID, BENEFICIARY_ID);

        when(claimSessionRepository.findByCaseIdAndBeneficiaryContactId("another-case", BENEFICIARY_ID))
                .thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> service.completeAccess(
                "another-case", session.sessionId(), BENEFICIARY_ID,
                new BeneficiaryFaceVerificationRequest(true, 0.96, 0.71, embedding(0.25))));
        assertEquals("PENDING_SCAN", claim.getKycFacialStatus());
    }

    @Test
    void enrollmentIsUnavailableBeforeOtpVerifiedActiveClaim() {
        when(claimSessionRepository
                .findTopByBeneficiaryContactIdAndTokenStatusAndIdentityVerifiedTrueOrderByCreatedAtDesc(
                        BENEFICIARY_ID, "ACTIVE"))
                .thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> service.getEnrollmentStatus(BENEFICIARY_ID));
        assertThrows(ResponseStatusException.class, () -> service.enroll(
                BENEFICIARY_ID, new BeneficiaryFaceEnrollmentRequest(true, embedding(0.25))));
    }

    private BeneficiaryClaimSession activeClaim() {
        BeneficiaryClaimSession claim = new BeneficiaryClaimSession();
        claim.setCaseId(CASE_ID);
        claim.setBeneficiaryContactId(BENEFICIARY_ID);
        claim.setIdentityVerified(true);
        claim.setTokenStatus("ACTIVE");
        claim.setKycFacialStatus("PENDING_SCAN");
        claim.setCreatedAt(LocalDateTime.now());
        return claim;
    }

    private void stubEnrollmentClaim(BeneficiaryClaimSession claim) {
        when(claimSessionRepository
                .findTopByBeneficiaryContactIdAndTokenStatusAndIdentityVerifiedTrueOrderByCreatedAtDesc(
                        BENEFICIARY_ID, "ACTIVE"))
                .thenReturn(Optional.of(claim));
    }

    private BeneficiaryFaceEnrollment enrolledProfile(double value) {
        FaceEmbeddingEncryptionService encryption = new FaceEmbeddingEncryptionService(TEST_KEY);
        return new BeneficiaryFaceEnrollment(BENEFICIARY_ID,
                encryption.encrypt(toPrimitive(embedding(value))),
                "human-3.3.6-face-description-v1", LocalDateTime.now());
    }

    private List<Double> embedding(double value) {
        return Collections.nCopies(128, value);
    }

    private double[] toPrimitive(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).toArray();
    }
}
