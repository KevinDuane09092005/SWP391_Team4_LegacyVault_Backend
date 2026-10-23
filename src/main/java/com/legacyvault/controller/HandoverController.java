package com.legacyvault.controller;

import com.legacyvault.dto.HandoverCaseResponse;
import com.legacyvault.service.HandoverService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/handover")
public class HandoverController {

    private final HandoverService handoverService;

    public HandoverController(HandoverService handoverService) {
        this.handoverService = handoverService;
    }

    @PostMapping("/trigger")
    public ResponseEntity<?> triggerHandover(
            Authentication authentication,
            @RequestBody(required = false) Map<String, String> body
    ) {
        try {
            String ownerId = null;
            if (body != null && body.containsKey("owner_id") && body.get("owner_id") != null && !body.get("owner_id").trim().isEmpty()) {
                ownerId = body.get("owner_id").trim();
            } else if (authentication != null && authentication.getName() != null) {
                ownerId = authentication.getName();
            }
            com.legacyvault.entity.HandoverCase savedCase = handoverService.triggerHandover(ownerId);
            return ResponseEntity.ok(Map.of(
                    "message", "Handover case triggered successfully",
                    "case_id", savedCase.getCaseId(),
                    "case_number", savedCase.getCaseNumber(),
                    "owner_id", savedCase.getOwnerId(),
                    "dms_id", savedCase.getDmsId(),
                    "status", savedCase.getStatus(),
                    "triggered_at", savedCase.getTriggeredAt()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/cases")
    public ResponseEntity<?> getHandoverCases(Authentication authentication) {
        try {
            String ownerId = getAuthenticatedUserId(authentication);
            List<HandoverCaseResponse> cases = handoverService.getCasesByOwner(ownerId);
            return ResponseEntity.ok(cases);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private String getAuthenticatedUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("Unauthorized: No user session found");
        }
        return authentication.getName();
    }
}
