package com.legacyvault.controller;

import com.legacyvault.dto.*;
import com.legacyvault.service.BeneficiaryHandoverService;
import com.legacyvault.service.HandoverExecutionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/handover")
public class AssetHandoverController {

    private final HandoverExecutionService handoverExecutionService;
    private final BeneficiaryHandoverService beneficiaryHandoverService;

    public AssetHandoverController(HandoverExecutionService handoverExecutionService,
                                   BeneficiaryHandoverService beneficiaryHandoverService) {
        this.handoverExecutionService = handoverExecutionService;
        this.beneficiaryHandoverService = beneficiaryHandoverService;
    }

    // =========================================================================
    // 1. EXECUTOR CONSOLE (DASHBOARD & DETAILS)
    // =========================================================================

    @GetMapping("/executor/dashboard")
    public ResponseEntity<?> getExecutorHandoverDashboard(Authentication authentication) {
        try {
            String userId = authentication != null ? authentication.getName() : "executor-user-id";
            ExecutorHandoverDashboardResponse response = handoverExecutionService.getExecutorDashboard(userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/cases/{caseId}/details")
    public ResponseEntity<?> getHandoverDetails(@PathVariable String caseId) {
        try {
            HandoverDetailsResponse response = handoverExecutionService.getHandoverDetails(caseId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/cases/{caseId}/start-release")
    public ResponseEntity<?> startHandoverRelease(@PathVariable String caseId, Authentication authentication) {
        try {
            String executorUserId = authentication != null ? authentication.getName() : "Minh Le (Executor)";
            StartReleaseResponse response = handoverExecutionService.startHandoverRelease(caseId, executorUserId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // =========================================================================
    // 2. BENEFICIARY PORTAL AUTHENTICATION & KYC
    // =========================================================================

    @GetMapping("/beneficiary/portal-context")
    public ResponseEntity<?> getBeneficiaryPortalContext(@RequestParam(name = "case_ref", defaultValue = "LV-1042") String caseRef) {
        try {
            BeneficiaryPortalContextResponse response = beneficiaryHandoverService.getPortalContext(caseRef);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/beneficiary/login-with-code")
    public ResponseEntity<?> beneficiaryLoginWithCode(@Valid @RequestBody BeneficiaryLoginRequest request) {
        try {
            BeneficiaryLoginSuccessResponse response = beneficiaryHandoverService.loginWithCode(
                    request.getEmail(), request.getAccessCode());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/beneficiary/verify-otp")
    public ResponseEntity<?> verifyBeneficiaryOtp(
            @Valid @RequestBody VerifyBeneficiaryOtpRequest request,
            @RequestHeader(name = "Authorization", required = false) String authHeader) {
        try {
            String sessionToken = authHeader != null && authHeader.startsWith("Bearer ")
                    ? authHeader.substring(7)
                    : "valid_session_token";
            VerifyBeneficiaryOtpResponse response = beneficiaryHandoverService.verifyOtp(sessionToken, request.getOtpCode());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/beneficiary/kyc-status")
    public ResponseEntity<?> getBeneficiaryKycStatus(Authentication authentication) {
        try {
            String contactId = authentication != null ? authentication.getName() : "beneficiary-contact-uuid";
            KycStatusResponse response = beneficiaryHandoverService.getKycStatus(contactId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // =========================================================================
    // 3. AUTHORIZATION EVALUATION (STEP 3)
    // =========================================================================

    @GetMapping("/cases/{caseId}/authorization-evaluation")
    public ResponseEntity<?> getAuthorizationEvaluation(@PathVariable String caseId) {
        try {
            AuthorizationEvaluationResponse response = handoverExecutionService.getAuthorizationEvaluation(caseId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/cases/{caseId}/dispatch")
    public ResponseEntity<?> dispatchSecureHandover(@PathVariable String caseId, Authentication authentication) {
        try {
            String executorUserId = authentication != null ? authentication.getName() : "Minh Le (Executor)";
            Map<String, String> result = handoverExecutionService.dispatchSecureHandover(caseId, executorUserId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // =========================================================================
    // 4. BENEFICIARY ASSET ACCESS & DECRYPTION (SCREENS 7 & 8)
    // =========================================================================

    @GetMapping("/beneficiary/authorized-assets")
    public ResponseEntity<?> getAuthorizedInheritanceAssets(Authentication authentication) {
        try {
            String contactId = authentication != null ? authentication.getName() : "beneficiary-contact-uuid";
            AuthorizedInheritanceAssetsResponse response = beneficiaryHandoverService.getAuthorizedAssets(contactId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/beneficiary/assets/{assetId}/detail")
    public ResponseEntity<?> getDecryptedAssetDetail(@PathVariable String assetId, Authentication authentication) {
        try {
            String contactId = authentication != null ? authentication.getName() : "beneficiary-contact-uuid";
            DecryptedAssetDetailResponse response = beneficiaryHandoverService.getDecryptedAssetDetail(assetId, contactId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/beneficiary/assets/{assetId}/acknowledge")
    public ResponseEntity<?> acknowledgeIndividualAsset(@PathVariable String assetId, Authentication authentication) {
        try {
            String contactId = authentication != null ? authentication.getName() : "beneficiary-contact-uuid";
            beneficiaryHandoverService.acknowledgeAsset(assetId, contactId);
            return ResponseEntity.ok(Map.of("message", "Item receipt acknowledged", "asset_id", assetId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // =========================================================================
    // 5. FORMAL RECEIPT & COMPLETION (SCREENS 9 & 10)
    // =========================================================================

    @GetMapping("/beneficiary/acknowledgement-summary")
    public ResponseEntity<?> getAcknowledgementSummary(Authentication authentication) {
        try {
            String contactId = authentication != null ? authentication.getName() : "beneficiary-contact-uuid";
            FormalAcknowledgementSummaryResponse response = beneficiaryHandoverService.getAcknowledgementSummary(contactId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/beneficiary/confirm-received")
    public ResponseEntity<?> confirmReceivedAndCloseCase(
            @Valid @RequestBody ConfirmReceivedRequest request,
            Authentication authentication,
            HttpServletRequest httpServletRequest) {
        try {
            String contactId = authentication != null ? authentication.getName() : "beneficiary-contact-uuid";
            String ipAddress = httpServletRequest.getRemoteAddr();
            String userAgent = httpServletRequest.getHeader("User-Agent");

            HandoverCompletedResponse response = beneficiaryHandoverService.confirmReceivedAndCloseCase(
                    contactId,
                    Boolean.TRUE.equals(request.getAcceptDigitalEstateCustody()),
                    ipAddress,
                    userAgent
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/cases/{caseId}/completion-receipt")
    public ResponseEntity<?> getCompletionReceipt(@PathVariable String caseId) {
        try {
            HandoverCompletedResponse response = handoverExecutionService.getCompletionReceipt(caseId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
