package com.legacyvault.controller;

import com.legacyvault.dto.AssignmentResponse;
import com.legacyvault.dto.CreateAssignmentRequest;
import com.legacyvault.service.AssignmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/assignments")
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validateAssignment(
            Authentication authentication,
            @Valid @RequestBody CreateAssignmentRequest request
    ) {
        try {
            String ownerId = getAuthenticatedUserId(authentication);
            Map<String, Object> result = assignmentService.validateAssignment(ownerId, request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "valid", false,
                    "error", e.getMessage()
            ));
        }
    }

    @PostMapping
    public ResponseEntity<?> createAssignment(
            Authentication authentication,
            @Valid @RequestBody CreateAssignmentRequest request
    ) {
        try {
            String ownerId = getAuthenticatedUserId(authentication);
            AssignmentResponse assignment = assignmentService.createAssignment(ownerId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "Beneficiary assigned successfully",
                    "assignment", assignment
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getAssignments(Authentication authentication) {
        try {
            String ownerId = getAuthenticatedUserId(authentication);
            List<AssignmentResponse> assignments = assignmentService.getAssignmentsByOwner(ownerId);
            return ResponseEntity.ok(assignments);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/asset/{assetId}")
    public ResponseEntity<?> getAssignmentByAssetId(
            Authentication authentication,
            @PathVariable("assetId") String assetId
    ) {
        try {
            String ownerId = getAuthenticatedUserId(authentication);
            AssignmentResponse assignment = assignmentService.getAssignmentByAssetId(ownerId, assetId);
            return ResponseEntity.ok(assignment);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    private String getAuthenticatedUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("Unauthorized: No user session found");
        }
        return authentication.getName();
    }
}
