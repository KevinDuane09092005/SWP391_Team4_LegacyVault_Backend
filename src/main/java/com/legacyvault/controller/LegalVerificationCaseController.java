package com.legacyvault.controller;

import com.legacyvault.dto.ApiResponse;
import com.legacyvault.dto.CaseDocumentResponse;
import com.legacyvault.dto.CaseEventListResponse;
import com.legacyvault.dto.CaseListResponse;
import com.legacyvault.dto.CreateCaseRequest;
import com.legacyvault.dto.DecisionRequest;
import com.legacyvault.dto.DocumentListResponse;
import com.legacyvault.dto.LegalVerificationCaseResponse;
import com.legacyvault.dto.RequestInformationRequest;
import com.legacyvault.entity.CaseStatus;
import com.legacyvault.entity.DocumentType;
import com.legacyvault.service.AuthenticatedActorProvider;
import com.legacyvault.service.LegalVerificationService;
import com.legacyvault.service.StoredContent;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/legal-verification-cases")
public class LegalVerificationCaseController {

    private final LegalVerificationService service;
    private final AuthenticatedActorProvider actorProvider;

    public LegalVerificationCaseController(LegalVerificationService service,
                                           AuthenticatedActorProvider actorProvider) {
        this.service = service;
        this.actorProvider = actorProvider;
    }

    @GetMapping
    public CaseListResponse listCases(@RequestParam(required = false) CaseStatus status,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size,
                                      HttpServletRequest request) {
        return service.listCases(actorProvider.requireActor(request), status, page, size);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<LegalVerificationCaseResponse>> createCase(
            @Valid @RequestBody CreateCaseRequest body, HttpServletRequest request) {
        LegalVerificationCaseResponse created = service.createDraft(
                actorProvider.requireActor(request), body.handoverCaseId(),
                body.assignmentId(), body.beneficiaryContactId());
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(created));
    }

    @GetMapping("/{caseId}")
    public ApiResponse<LegalVerificationCaseResponse> getCase(@PathVariable String caseId,
                                                              HttpServletRequest request) {
        return new ApiResponse<>(service.getCase(actorProvider.requireActor(request), caseId));
    }

    @GetMapping("/{caseId}/documents")
    public DocumentListResponse listDocuments(@PathVariable String caseId, HttpServletRequest request) {
        return service.listDocuments(actorProvider.requireActor(request), caseId);
    }

    @PostMapping(value = "/{caseId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CaseDocumentResponse>> uploadDocument(
            @PathVariable String caseId,
            @RequestParam DocumentType documentType,
            @RequestParam MultipartFile file,
            HttpServletRequest request) {
        CaseDocumentResponse document = service.uploadDocument(
                actorProvider.requireActor(request), caseId, documentType, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(document));
    }

    @GetMapping("/{caseId}/documents/{documentId}/content")
    public ResponseEntity<byte[]> readDocument(@PathVariable String caseId,
                                               @PathVariable String documentId,
                                               HttpServletRequest request) {
        StoredContent content = service.readDocument(actorProvider.requireActor(request), caseId, documentId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.mediaType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(content.fileName(), StandardCharsets.UTF_8).build().toString())
                .body(content.bytes());
    }

    @PostMapping("/{caseId}/submit")
    public ApiResponse<LegalVerificationCaseResponse> submit(@PathVariable String caseId,
                                                             HttpServletRequest request) {
        return new ApiResponse<>(service.submit(actorProvider.requireActor(request), caseId));
    }

    @PostMapping("/{caseId}/start-review")
    public ApiResponse<LegalVerificationCaseResponse> startReview(@PathVariable String caseId,
                                                                 HttpServletRequest request) {
        return new ApiResponse<>(service.startReview(actorProvider.requireActor(request), caseId));
    }

    @PostMapping("/{caseId}/request-information")
    public ApiResponse<LegalVerificationCaseResponse> requestInformation(
            @PathVariable String caseId, @Valid @RequestBody RequestInformationRequest body,
            HttpServletRequest request) {
        return new ApiResponse<>(service.requestInformation(
                actorProvider.requireActor(request), caseId, body));
    }

    @PostMapping("/{caseId}/decision")
    public ApiResponse<LegalVerificationCaseResponse> decide(@PathVariable String caseId,
                                                             @Valid @RequestBody DecisionRequest body,
                                                             HttpServletRequest request) {
        return new ApiResponse<>(service.decide(actorProvider.requireActor(request), caseId, body));
    }

    @GetMapping("/{caseId}/events")
    public CaseEventListResponse listEvents(@PathVariable String caseId, HttpServletRequest request) {
        return service.listEvents(actorProvider.requireActor(request), caseId);
    }
}
