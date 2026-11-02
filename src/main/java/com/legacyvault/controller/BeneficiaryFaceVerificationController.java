package com.legacyvault.controller;

import com.legacyvault.dto.BeneficiaryFaceCheckSessionResponse;
import com.legacyvault.dto.BeneficiaryFaceEnrollmentRequest;
import com.legacyvault.dto.BeneficiaryFaceEnrollmentStatusResponse;
import com.legacyvault.dto.BeneficiaryFaceVerificationRequest;
import com.legacyvault.dto.BeneficiaryFaceVerificationResponse;
import com.legacyvault.service.BeneficiaryFaceVerificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/handover/beneficiary/face")
public class BeneficiaryFaceVerificationController {

    private final BeneficiaryFaceVerificationService service;

    public BeneficiaryFaceVerificationController(BeneficiaryFaceVerificationService service) {
        this.service = service;
    }

    @GetMapping("/enrollment")
    public BeneficiaryFaceEnrollmentStatusResponse enrollmentStatus(Authentication authentication) {
        return service.getEnrollmentStatus(beneficiaryId(authentication));
    }

    @PutMapping("/enrollment")
    public BeneficiaryFaceEnrollmentStatusResponse enroll(
            @Valid @RequestBody BeneficiaryFaceEnrollmentRequest request,
            Authentication authentication) {
        return service.enroll(beneficiaryId(authentication), request);
    }

    @DeleteMapping("/enrollment")
    public ResponseEntity<Void> deleteEnrollment(Authentication authentication) {
        service.deleteEnrollment(beneficiaryId(authentication));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/cases/{caseId}/sessions")
    public ResponseEntity<BeneficiaryFaceCheckSessionResponse> createAccessSession(
            @PathVariable String caseId,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createAccessSession(caseId, beneficiaryId(authentication)));
    }

    @PostMapping("/cases/{caseId}/sessions/{sessionId}/complete")
    public BeneficiaryFaceVerificationResponse completeAccess(
            @PathVariable String caseId,
            @PathVariable String sessionId,
            @Valid @RequestBody BeneficiaryFaceVerificationRequest request,
            Authentication authentication) {
        return service.completeAccess(caseId, sessionId, beneficiaryId(authentication), request);
    }

    private String beneficiaryId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null
                || "anonymousUser".equals(authentication.getName())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Beneficiary authentication is required");
        }
        return authentication.getName();
    }
}
