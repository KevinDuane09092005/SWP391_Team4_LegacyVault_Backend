package com.legacyvault.service;

import com.legacyvault.dto.BeneficiaryFaceCheckSessionResponse;
import com.legacyvault.dto.BeneficiaryFaceEnrollmentRequest;
import com.legacyvault.dto.BeneficiaryFaceEnrollmentStatusResponse;
import com.legacyvault.dto.BeneficiaryFaceVerificationRequest;
import com.legacyvault.dto.BeneficiaryFaceVerificationResponse;
import com.legacyvault.entity.BeneficiaryClaimSession;
import com.legacyvault.entity.BeneficiaryFaceEnrollment;
import com.legacyvault.repository.BeneficiaryClaimSessionRepository;
import com.legacyvault.repository.BeneficiaryFaceEnrollmentRepository;
import com.legacyvault.repository.HandoverCaseRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class BeneficiaryFaceVerificationService {

    private static final String ACTIVE = "ACTIVE";
    private static final String PENDING_SCAN = "PENDING_SCAN";
    private static final String VERIFIED = "VERIFIED";
    private static final String FAILED = "FAILED";
    private static final String EMBEDDING_VERSION = "human-3.3.6-face-description-v1";
    private static final int SESSION_LIFETIME_SECONDS = 600;
    private static final int MAX_ACTIVE_SESSIONS = 500;
    private static final int EMBEDDING_MIN_LENGTH = 64;
    private static final int EMBEDDING_MAX_LENGTH = 4096;
    private static final double LIVENESS_THRESHOLD = 0.6;
    private static final double ANTI_SPOOF_THRESHOLD = 0.6;
    private static final double FACE_MATCH_THRESHOLD = 0.5;

    private final BeneficiaryFaceEnrollmentRepository enrollmentRepository;
    private final BeneficiaryClaimSessionRepository claimSessionRepository;
    private final HandoverCaseRepository handoverCaseRepository;
    private final FaceEmbeddingEncryptionService encryptionService;
    private final ConcurrentMap<UUID, FaceCheckSession> sessions = new ConcurrentHashMap<>();

    public BeneficiaryFaceVerificationService(
            BeneficiaryFaceEnrollmentRepository enrollmentRepository,
            BeneficiaryClaimSessionRepository claimSessionRepository,
            HandoverCaseRepository handoverCaseRepository,
            FaceEmbeddingEncryptionService encryptionService) {
        this.enrollmentRepository = enrollmentRepository;
        this.claimSessionRepository = claimSessionRepository;
        this.handoverCaseRepository = handoverCaseRepository;
        this.encryptionService = encryptionService;
    }

    @Transactional(readOnly = true)
    public BeneficiaryFaceEnrollmentStatusResponse getEnrollmentStatus(String beneficiaryContactId) {
        requireLatestActiveClaim(beneficiaryContactId);
        return enrollmentRepository.findByBeneficiaryContactId(beneficiaryContactId)
                .map(profile -> new BeneficiaryFaceEnrollmentStatusResponse(true, profile.getConsentedAt()))
                .orElseGet(() -> new BeneficiaryFaceEnrollmentStatusResponse(false, null));
    }

    @Transactional
    public BeneficiaryFaceEnrollmentStatusResponse enroll(
            String beneficiaryContactId, BeneficiaryFaceEnrollmentRequest request) {
        requireLatestActiveClaim(beneficiaryContactId);
        if (!request.consent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Consent is required before saving a face reference");
        }

        double[] embedding = validateAndConvertEmbedding(request.embedding());
        byte[] encrypted = encryptionService.encrypt(embedding);
        LocalDateTime now = LocalDateTime.now();
        BeneficiaryFaceEnrollment profile = enrollmentRepository
                .findByBeneficiaryContactId(beneficiaryContactId)
                .orElseGet(() -> new BeneficiaryFaceEnrollment(
                        beneficiaryContactId, encrypted, EMBEDDING_VERSION, now));
        profile.setEncryptedEmbedding(encrypted);
        profile.setEmbeddingVersion(EMBEDDING_VERSION);
        profile.setConsentedAt(now);
        enrollmentRepository.save(profile);

        resetActiveClaimStatuses(beneficiaryContactId);
        return new BeneficiaryFaceEnrollmentStatusResponse(true, now);
    }

    @Transactional
    public void deleteEnrollment(String beneficiaryContactId) {
        requireLatestActiveClaim(beneficiaryContactId);
        enrollmentRepository.deleteByBeneficiaryContactId(beneficiaryContactId);
        resetActiveClaimStatuses(beneficiaryContactId);
    }

    @Transactional
    public BeneficiaryFaceCheckSessionResponse createAccessSession(
            String caseReference, String beneficiaryContactId) {
        BeneficiaryClaimSession claim = requireActiveClaim(caseReference, beneficiaryContactId);
        enrollmentRepository.findByBeneficiaryContactId(beneficiaryContactId)
                .filter(profile -> profile.getEncryptedEmbedding() != null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "Enroll a face reference before starting the access check"));

        removeExpiredSessions();
        sessions.entrySet().removeIf(entry -> entry.getValue().matches(claim.getCaseId(), beneficiaryContactId));
        if (sessions.size() >= MAX_ACTIVE_SESSIONS) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Face verification is busy; wait a moment and try again");
        }

        UUID sessionId = UUID.randomUUID();
        String direction = ThreadLocalRandom.current().nextBoolean() ? "left" : "right";
        Instant expiresAt = Instant.now().plusSeconds(SESSION_LIFETIME_SECONDS);
        sessions.put(sessionId, new FaceCheckSession(claim.getCaseId(), beneficiaryContactId,
                direction, expiresAt));
        claim.setKycFacialStatus(PENDING_SCAN);
        claimSessionRepository.save(claim);
        return new BeneficiaryFaceCheckSessionResponse(
                sessionId.toString(), direction, SESSION_LIFETIME_SECONDS);
    }

    @Transactional
    public BeneficiaryFaceVerificationResponse completeAccess(
            String caseReference,
            String sessionIdValue,
            String beneficiaryContactId,
            BeneficiaryFaceVerificationRequest request) {
        UUID sessionId = parseSessionId(sessionIdValue);
        FaceCheckSession session = sessions.get(sessionId);
        if (session == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Face verification session was not found; start a new scan");
        }

        BeneficiaryClaimSession claim = requireActiveClaim(caseReference, beneficiaryContactId);
        if (!session.matches(claim.getCaseId(), beneficiaryContactId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Face verification session was not found for this beneficiary and case");
        }
        if (Instant.now().isAfter(session.expiresAt())) {
            sessions.remove(sessionId, session);
            throw new ResponseStatusException(HttpStatus.GONE,
                    "Face verification session expired; start a new scan");
        }
        if (!sessions.remove(sessionId, session)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Face verification session has already been used");
        }

        double[] liveEmbedding = validateAndConvertEmbedding(request.embedding());
        BeneficiaryFaceEnrollment profile = enrollmentRepository
                .findByBeneficiaryContactId(beneficiaryContactId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "No enrolled face reference is available"));
        if (!EMBEDDING_VERSION.equals(profile.getEmbeddingVersion())) {
            claim.setKycFacialStatus(FAILED);
            claimSessionRepository.save(claim);
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Face reference uses an older model; enroll again before continuing");
        }

        double[] referenceEmbedding = encryptionService.decrypt(profile.getEncryptedEmbedding());
        double matchScore = similarity(referenceEmbedding, liveEmbedding);
        boolean challengePassed = request.challengeCompleted();
        double livenessScore = request.livenessScore();
        double antiSpoofScore = request.antiSpoofScore();
        boolean livenessPassed = challengePassed
                && isPassingScore(livenessScore, LIVENESS_THRESHOLD)
                && isPassingScore(antiSpoofScore, ANTI_SPOOF_THRESHOLD);
        boolean matchPassed = matchScore >= FACE_MATCH_THRESHOLD;
        boolean passed = livenessPassed && matchPassed;

        claim.setKycFacialStatus(passed ? VERIFIED : FAILED);
        claimSessionRepository.save(claim);

        String message = passed
                ? "Face match and liveness checks passed. Existing Flow 6 authorization still applies."
                : !livenessPassed
                    ? "The liveness challenge did not pass. Try again in better lighting."
                    : "The live face did not match the enrolled beneficiary reference closely enough.";
        return new BeneficiaryFaceVerificationResponse(
                passed ? VERIFIED : FAILED,
                passed,
                challengePassed,
                livenessScore,
                antiSpoofScore,
                matchScore,
                message,
                true);
    }

    private BeneficiaryClaimSession requireLatestActiveClaim(String beneficiaryContactId) {
        return claimSessionRepository
                .findTopByBeneficiaryContactIdAndTokenStatusAndIdentityVerifiedTrueOrderByCreatedAtDesc(
                        beneficiaryContactId, ACTIVE)
                .filter(claim -> ACTIVE.equalsIgnoreCase(claim.getTokenStatus()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Verify the beneficiary one-time code before face enrollment"));
    }

    private BeneficiaryClaimSession requireActiveClaim(String caseReference, String beneficiaryContactId) {
        Optional<BeneficiaryClaimSession> claim = claimSessionRepository
                .findByCaseIdAndBeneficiaryContactId(caseReference, beneficiaryContactId);
        if (claim.isEmpty()) {
            claim = handoverCaseRepository.findByCaseNumber(caseReference)
                    .flatMap(handoverCase -> claimSessionRepository.findByCaseIdAndBeneficiaryContactId(
                            handoverCase.getCaseId(), beneficiaryContactId));
        }
        return claim
                .filter(item -> Boolean.TRUE.equals(item.getIdentityVerified()))
                .filter(item -> ACTIVE.equalsIgnoreCase(item.getTokenStatus()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Complete beneficiary sign-in and one-time-code verification for this case first"));
    }

    private void resetActiveClaimStatuses(String beneficiaryContactId) {
        List<BeneficiaryClaimSession> claims = claimSessionRepository
                .findByBeneficiaryContactIdAndTokenStatus(beneficiaryContactId, ACTIVE);
        claims.forEach(claim -> claim.setKycFacialStatus(PENDING_SCAN));
        claimSessionRepository.saveAll(claims);
    }

    private double[] validateAndConvertEmbedding(List<Double> values) {
        if (values == null || values.size() < EMBEDDING_MIN_LENGTH
                || values.size() > EMBEDDING_MAX_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A face embedding with 64 to 4096 values is required");
        }
        double[] result = new double[values.size()];
        for (int i = 0; i < values.size(); i++) {
            Double value = values.get(i);
            if (value == null || !Double.isFinite(value) || Math.abs(value) > 10) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Face embedding contains an invalid value");
            }
            result[i] = value;
        }
        return result;
    }

    private boolean isPassingScore(double score, double threshold) {
        return Double.isFinite(score) && score >= threshold && score <= 1;
    }

    /** Mirrors the Human library's configured L2 similarity conversion in the local camera demo. */
    private double similarity(double[] reference, double[] current) {
        if (reference.length != current.length) {
            return 0;
        }
        double squaredDistance = 0;
        for (int i = 0; i < reference.length; i++) {
            double difference = reference[i] - current[i];
            squaredDistance += difference * difference;
        }
        double distance = Math.round(100 * 25 * squaredDistance) / 100.0;
        if (distance == 0) {
            return 1;
        }
        double normalized = (1 - Math.sqrt(distance) / 100 - 0.2) / (0.8 - 0.2);
        return Math.round(Math.max(0, Math.min(1, normalized)) * 100) / 100.0;
    }

    private void removeExpiredSessions() {
        Instant now = Instant.now();
        sessions.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expiresAt()));
    }

    private UUID parseSessionId(String value) {
        try {
            return UUID.fromString(value);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Face verification session was not found; start a new scan");
        }
    }

    private record FaceCheckSession(String caseId, String beneficiaryContactId,
                                    String challengeDirection, Instant expiresAt) {
        private boolean matches(String expectedCaseId, String expectedBeneficiaryContactId) {
            return caseId.equals(expectedCaseId) && beneficiaryContactId.equals(expectedBeneficiaryContactId);
        }
    }
}
