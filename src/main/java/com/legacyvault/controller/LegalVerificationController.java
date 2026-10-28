package com.legacyvault.controller;

import com.legacyvault.dto.*;
import com.legacyvault.service.LegalVerificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v1/legal-verification")
public class LegalVerificationController {

    private final LegalVerificationService legalVerificationService;

    public LegalVerificationController(LegalVerificationService legalVerificationService) {
        this.legalVerificationService = legalVerificationService;
    }

    /**
     * Màn hình chính "Legal Verification Queue" (UI Prototype Flow 5 / Flow 5b)
     */
    @GetMapping("/verifier/queue")
    public ResponseEntity<?> getVerificationQueue(
            @RequestParam(required = false, defaultValue = "ALL") String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size) {
        try {
            Page<VerificationQueueItemResponse> result = legalVerificationService.getVerificationQueue(
                    status, search, PageRequest.of(page, size));
            return ResponseEntity.ok(Map.of(
                    "content", result.getContent(),
                    "total_elements", result.getTotalElements(),
                    "total_pages", result.getTotalPages(),
                    "page_number", result.getNumber()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Xem chi tiết Case (vùng mở rộng "Case #LV-XXXX - Detail" trên UI)
     */
    @GetMapping("/cases/{caseId}")
    public ResponseEntity<?> getCaseDetail(@PathVariable String caseId) {
        try {
            VerificationCaseDetailResponse detail = legalVerificationService.getCaseDetail(caseId);
            return ResponseEntity.ok(detail);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Verifier tiếp nhận thẩm định Case (chuyển sang UNDER_REVIEW)
     */
    @PostMapping("/cases/{caseId}/start-review")
    public ResponseEntity<?> startReview(@PathVariable String caseId, Authentication authentication) {
        try {
            String verifierId = authentication != null ? authentication.getName() : "verifier-demo";
            VerificationCaseDetailResponse detail = legalVerificationService.startReview(caseId, verifierId);
            return ResponseEntity.ok(detail);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Nút "Approve & Sign" trên màn hình Flow 5 (chuyển sang UI Flow 5b Approved)
     */
    @PostMapping("/cases/{caseId}/approve")
    public ResponseEntity<?> approveCase(
            @PathVariable String caseId,
            @RequestBody(required = false) ApproveCaseRequest request,
            Authentication authentication) {
        try {
            String verifierId = authentication != null ? authentication.getName() : "verifier-demo";
            ApproveCaseResponse response = legalVerificationService.approveCase(caseId, verifierId, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Nút "Reject" màu đỏ trên màn hình Flow 5
     */
    @PostMapping("/cases/{caseId}/reject")
    public ResponseEntity<?> rejectCase(
            @PathVariable String caseId,
            @RequestBody(required = false) RejectCaseRequest request,
            Authentication authentication) {
        try {
            String verifierId = authentication != null ? authentication.getName() : "verifier-demo";
            VerificationCaseDetailResponse detail = legalVerificationService.rejectCase(caseId, verifierId, request);
            return ResponseEntity.ok(detail);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Executor upload Giấy chứng tử (Death Certificate)
     */
    @PostMapping("/cases/{caseId}/documents")
    public ResponseEntity<?> uploadDocument(
            @PathVariable String caseId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "document_type", required = false, defaultValue = "DEATH_CERTIFICATE") String documentType,
            @RequestParam(value = "document_number", required = false) String documentNumber,
            @RequestParam(value = "issued_by", required = false) String issuedBy,
            @RequestParam(value = "issued_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate issuedDate) {
        try {
            VerificationDocumentResponse doc = legalVerificationService.uploadDocument(
                    caseId, file, documentType, documentNumber, issuedBy, issuedDate);
            return ResponseEntity.status(HttpStatus.CREATED).body(doc);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Executor nộp hồ sơ yêu cầu thẩm định pháp lý (Submit Verification Request)
     */
    @PostMapping("/cases/{caseId}/submit")
    public ResponseEntity<?> submitVerificationRequest(
            @PathVariable String caseId,
            @RequestBody(required = false) Map<String, String> body) {
        try {
            String notes = body != null ? body.get("submission_notes") : null;
            VerificationCaseDetailResponse detail = legalVerificationService.submitVerificationRequest(caseId, notes);
            return ResponseEntity.ok(detail);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Preview document inline (PDF / PNG / JPG) for Frontend iframe or modal
     */
    @GetMapping("/documents/{documentId}/view")
    public ResponseEntity<Resource> viewDocument(@PathVariable String documentId) {
        try {
            LegalVerificationService.DocumentResourceInfo docInfo = legalVerificationService.getDocumentResource(documentId);
            ContentDisposition contentDisposition = ContentDisposition.inline()
                    .filename(docInfo.getFileName(), StandardCharsets.UTF_8)
                    .build();

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(docInfo.getContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                    .body(docInfo.getResource());
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Download document as attachment
     */
    @GetMapping({"/documents/{documentId}/download", "/documents/download/{documentId}"})
    public ResponseEntity<Resource> downloadDocument(@PathVariable String documentId) {
        try {
            LegalVerificationService.DocumentResourceInfo docInfo = legalVerificationService.getDocumentResource(documentId);
            ContentDisposition contentDisposition = ContentDisposition.attachment()
                    .filename(docInfo.getFileName(), StandardCharsets.UTF_8)
                    .build();

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(docInfo.getContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                    .body(docInfo.getResource());
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
