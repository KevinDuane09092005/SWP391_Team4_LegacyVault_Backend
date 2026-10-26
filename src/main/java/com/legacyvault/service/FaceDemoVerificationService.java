package com.legacyvault.service;

import com.legacyvault.dto.FaceDemoEnrollmentRequest;
import com.legacyvault.dto.FaceDemoEnrollmentResponse;
import com.legacyvault.dto.FaceDemoSessionResponse;
import com.legacyvault.dto.FaceDemoVerificationRequest;
import com.legacyvault.dto.FaceDemoVerificationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Local-only demo service. Templates live in process memory and are never written to the database.
 * The browser supplies the liveness signals, so this service is not a trusted biometric boundary.
 */
@Service
@Profile("face-demo")
public class FaceDemoVerificationService {

    private static final int MAX_SESSIONS = 500;
    private static final int EMBEDDING_MIN_LENGTH = 64;
    private static final int EMBEDDING_MAX_LENGTH = 4096;
    private static final double DISTANCE_MULTIPLIER = 25;
    private static final double SIMILARITY_MIN = 0.2;
    private static final double SIMILARITY_MAX = 0.8;

    private final ConcurrentMap<UUID, DemoSession> sessions = new ConcurrentHashMap<>();
    private final Duration sessionLifetime;
    private final double livenessThreshold;
    private final double antiSpoofThreshold;
    private final double faceMatchThreshold;

    public FaceDemoVerificationService(
            @Value("${face-verification.demo.session-minutes:15}") long sessionMinutes,
            @Value("${face-verification.demo.liveness-threshold:0.6}") double livenessThreshold,
            @Value("${face-verification.demo.anti-spoof-threshold:0.6}") double antiSpoofThreshold,
            @Value("${face-verification.demo.face-match-threshold:0.5}") double faceMatchThreshold) {
        this.sessionLifetime = Duration.ofMinutes(sessionMinutes);
        this.livenessThreshold = livenessThreshold;
        this.antiSpoofThreshold = antiSpoofThreshold;
        this.faceMatchThreshold = faceMatchThreshold;
    }

    public FaceDemoSessionResponse createSession() {
        removeExpiredSessions();
        if (sessions.size() >= MAX_SESSIONS) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "The local face demo is busy; wait a moment and try again");
        }
        UUID id = UUID.randomUUID();
        sessions.put(id, new DemoSession(Instant.now().plus(sessionLifetime)));
        return new FaceDemoSessionResponse(id, Math.toIntExact(sessionLifetime.toSeconds()));
    }

    public FaceDemoEnrollmentResponse enroll(UUID sessionId, FaceDemoEnrollmentRequest request) {
        if (!request.consent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Consent is required before enrolling a face reference");
        }
        double[] embedding = validateAndConvertEmbedding(request.embedding());
        DemoSession session = requireSession(sessionId);
        session.referenceEmbedding = embedding;
        return new FaceDemoEnrollmentResponse(true,
                "The face template is held in backend memory for this local demo session only");
    }

    public FaceDemoVerificationResponse verify(UUID sessionId, FaceDemoVerificationRequest request) {
        double[] currentEmbedding = validateAndConvertEmbedding(request.embedding());
        DemoSession session = requireSession(sessionId);
        double[] referenceEmbedding = session.referenceEmbedding;
        if (referenceEmbedding == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Enroll a face reference before running verification");
        }

        double matchScore = similarity(referenceEmbedding, currentEmbedding);
        boolean livenessPassed = request.challengeCompleted()
                && request.livenessScore() >= livenessThreshold
                && request.antiSpoofScore() >= antiSpoofThreshold;
        boolean matchPassed = matchScore >= faceMatchThreshold;
        boolean passed = livenessPassed && matchPassed;
        String message = passed
                ? "The browser-reported liveness checks and backend face match passed. Demo only; no assets were accessed."
                : !livenessPassed
                    ? "The challenge or browser-reported liveness scores did not meet the demo thresholds."
                    : "The live face did not match the enrolled reference closely enough.";

        return new FaceDemoVerificationResponse(
                passed ? "PASSED" : "FAILED",
                passed,
                request.challengeCompleted(),
                request.livenessScore(),
                request.antiSpoofScore(),
                matchScore,
                message,
                true);
    }

    public void deleteSession(UUID sessionId) {
        sessions.remove(sessionId);
    }

    private DemoSession requireSession(UUID sessionId) {
        DemoSession session = sessions.get(sessionId);
        if (session == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Demo session was not found; start a new camera session");
        }
        if (Instant.now().isAfter(session.expiresAt)) {
            sessions.remove(sessionId, session);
            throw new ResponseStatusException(HttpStatus.GONE,
                    "Demo session expired; start a new camera session");
        }
        return session;
    }

    private void removeExpiredSessions() {
        Instant now = Instant.now();
        sessions.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expiresAt));
    }

    private double[] validateAndConvertEmbedding(List<Double> values) {
        if (values == null || values.size() < EMBEDDING_MIN_LENGTH || values.size() > EMBEDDING_MAX_LENGTH) {
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

    /** Mirrors Human.match.similarity using order=2, multiplier=25, min=.2, max=.8. */
    private double similarity(double[] reference, double[] current) {
        if (reference.length != current.length) return 0;
        double squaredDistance = 0;
        for (int i = 0; i < reference.length; i++) {
            double difference = reference[i] - current[i];
            squaredDistance += difference * difference;
        }
        double distance = Math.round(100 * DISTANCE_MULTIPLIER * squaredDistance) / 100.0;
        if (distance == 0) return 1;
        double normalized = (1 - Math.sqrt(distance) / 100 - SIMILARITY_MIN)
                / (SIMILARITY_MAX - SIMILARITY_MIN);
        double clamped = Math.max(0, Math.min(1, normalized));
        return Math.round(clamped * 100) / 100.0;
    }

    private static final class DemoSession {
        private final Instant expiresAt;
        private volatile double[] referenceEmbedding;

        private DemoSession(Instant expiresAt) {
            this.expiresAt = expiresAt;
        }
    }
}
