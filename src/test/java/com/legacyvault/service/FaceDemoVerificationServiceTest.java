package com.legacyvault.service;

import com.legacyvault.dto.FaceDemoEnrollmentRequest;
import com.legacyvault.dto.FaceDemoVerificationRequest;
import com.legacyvault.dto.FaceDemoVerificationResponse;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FaceDemoVerificationServiceTest {

    private final FaceDemoVerificationService service =
            new FaceDemoVerificationService(15, 0.6, 0.6, 0.5);

    @Test
    void sameFacePassesWhenTheChallengeAndLivenessSignalsPass() {
        var session = service.createSession();
        List<Double> embedding = Collections.nCopies(128, 0.25);
        service.enroll(session.sessionId(), new FaceDemoEnrollmentRequest(true, embedding));

        FaceDemoVerificationResponse result = service.verify(session.sessionId(),
                new FaceDemoVerificationRequest(true, 0.96, 0.71, embedding));

        assertThat(result.passed()).isTrue();
        assertThat(result.status()).isEqualTo("PASSED");
        assertThat(result.faceMatchScore()).isEqualTo(1);
    }

    @Test
    void differentFaceFailsEvenWhenItsLivenessSignalsPass() {
        var session = service.createSession();
        List<Double> reference = Collections.nCopies(128, 0.0);
        List<Double> differentFace = Collections.nCopies(128, 1.0);
        service.enroll(session.sessionId(), new FaceDemoEnrollmentRequest(true, reference));

        FaceDemoVerificationResponse result = service.verify(session.sessionId(),
                new FaceDemoVerificationRequest(true, 0.96, 0.71, differentFace));

        assertThat(result.passed()).isFalse();
        assertThat(result.livenessScore()).isEqualTo(0.96);
        assertThat(result.antiSpoofScore()).isEqualTo(0.71);
        assertThat(result.faceMatchScore()).isLessThan(0.5);
    }

    @Test
    void sameFaceFailsIfLivenessOrChallengeDoesNotPass() {
        var session = service.createSession();
        List<Double> embedding = Collections.nCopies(128, 0.25);
        service.enroll(session.sessionId(), new FaceDemoEnrollmentRequest(true, embedding));

        FaceDemoVerificationResponse result = service.verify(session.sessionId(),
                new FaceDemoVerificationRequest(false, 0.55, 0.99, embedding));

        assertThat(result.passed()).isFalse();
        assertThat(result.faceMatchScore()).isEqualTo(1);
    }
}
