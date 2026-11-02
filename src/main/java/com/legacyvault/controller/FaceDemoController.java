package com.legacyvault.controller;

import com.legacyvault.dto.FaceDemoEnrollmentRequest;
import com.legacyvault.dto.FaceDemoEnrollmentResponse;
import com.legacyvault.dto.FaceDemoSessionResponse;
import com.legacyvault.dto.FaceDemoVerificationRequest;
import com.legacyvault.dto.FaceDemoVerificationResponse;
import com.legacyvault.service.FaceDemoVerificationService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/demo/face")
@Profile("face-demo")
public class FaceDemoController {

    private final FaceDemoVerificationService service;

    public FaceDemoController(FaceDemoVerificationService service) {
        this.service = service;
    }

    @PostMapping("/sessions")
    public ResponseEntity<FaceDemoSessionResponse> createSession() {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createSession());
    }

    @PostMapping("/sessions/{sessionId}/enrollment")
    public FaceDemoEnrollmentResponse enroll(
            @PathVariable UUID sessionId,
            @Valid @RequestBody FaceDemoEnrollmentRequest request) {
        return service.enroll(sessionId, request);
    }

    @PostMapping("/sessions/{sessionId}/verification")
    public FaceDemoVerificationResponse verify(
            @PathVariable UUID sessionId,
            @Valid @RequestBody FaceDemoVerificationRequest request) {
        return service.verify(sessionId, request);
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> deleteSession(@PathVariable UUID sessionId) {
        service.deleteSession(sessionId);
        return ResponseEntity.noContent().build();
    }
}
