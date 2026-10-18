package com.legacyvault.service;

import com.legacyvault.dto.Actor;
import com.legacyvault.dto.CaseDecisionResponse;
import com.legacyvault.dto.CaseDocumentResponse;
import com.legacyvault.dto.CaseEventListResponse;
import com.legacyvault.dto.CaseEventResponse;
import com.legacyvault.dto.CaseListResponse;
import com.legacyvault.dto.CaseSummaryResponse;
import com.legacyvault.dto.DecisionOutcome;
import com.legacyvault.dto.DecisionRequest;
import com.legacyvault.dto.DocumentListResponse;
import com.legacyvault.dto.LegalVerificationCaseResponse;
import com.legacyvault.dto.PageMeta;
import com.legacyvault.dto.RequestInformationRequest;
import com.legacyvault.entity.AttestationType;
import com.legacyvault.entity.CaseEventType;
import com.legacyvault.entity.CaseStatus;
import com.legacyvault.entity.DocumentType;
import com.legacyvault.entity.LegalVerificationCase;
import com.legacyvault.entity.LegalVerificationDocument;
import com.legacyvault.entity.LegalVerificationEvent;
import com.legacyvault.repository.LegalVerificationCaseRepository;
import com.legacyvault.repository.LegalVerificationDocumentRepository;
import com.legacyvault.repository.LegalVerificationEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class LegalVerificationService {

    private static final long MAX_FILE_BYTES = 10L * 1024 * 1024;

    private final LegalVerificationCaseRepository caseRepository;
    private final LegalVerificationDocumentRepository documentRepository;
    private final LegalVerificationEventRepository eventRepository;
    private final ActivationGateway activationGateway;
    private final DocumentStorage documentStorage;

    public LegalVerificationService(LegalVerificationCaseRepository caseRepository,
                                   LegalVerificationDocumentRepository documentRepository,
                                   LegalVerificationEventRepository eventRepository,
                                   ActivationGateway activationGateway,
                                   DocumentStorage documentStorage) {
        this.caseRepository = caseRepository;
        this.documentRepository = documentRepository;
        this.eventRepository = eventRepository;
        this.activationGateway = activationGateway;
        this.documentStorage = documentStorage;
    }

    public LegalVerificationCaseResponse createDraft(Actor actor, String handoverCaseId,
                                                     String assignmentId, String beneficiaryContactId) {
        requireExecutor(actor);
        VerificationTarget target = activationGateway.requireExecutorAssignment(
                handoverCaseId, assignmentId, beneficiaryContactId, actor.userId());
        if (caseRepository.existsByHandoverCase_CaseIdAndAssignment_AssignmentIdAndBeneficiaryContact_ContactId(
                handoverCaseId, assignmentId, beneficiaryContactId)) {
            throw Flow5Exception.conflict("A verification case already exists for this handover, assignment, and beneficiary.");
        }

        LegalVerificationCase verificationCase = new LegalVerificationCase(
                UUID.randomUUID().toString(), target.handoverCase(), target.assignment(),
                target.beneficiaryContact(), actor.userId());
        caseRepository.save(verificationCase);
        recordEvent(verificationCase, actor, CaseEventType.CASE_CREATED, null,
                CaseStatus.DRAFT, null, null);
        return toCaseResponse(verificationCase);
    }

    @Transactional(readOnly = true)
    public CaseListResponse listCases(Actor actor, CaseStatus status, int page, int size) {
        requireAnyRole(actor);
        if (page < 0 || size < 1 || size > 100) {
            throw new Flow5Exception(HttpStatus.BAD_REQUEST, "INVALID_PAGE", "Page must be non-negative and size must be between 1 and 100.");
        }
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<LegalVerificationCase> result;
        if (actor.legalVerifier()) {
            result = caseRepository.findVisibleToReviewer(
                    actor.userId(), CaseStatus.SUBMITTED, status, pageRequest);
        } else {
            result = status == null
                    ? caseRepository.findByExecutorUserId(actor.userId(), pageRequest)
                    : caseRepository.findByExecutorUserIdAndStatus(actor.userId(), status, pageRequest);
        }
        return new CaseListResponse(result.getContent().stream().map(this::toSummary).toList(),
                new PageMeta(page, size, result.getTotalElements()));
    }

    @Transactional(readOnly = true)
    public LegalVerificationCaseResponse getCase(Actor actor, String caseId) {
        LegalVerificationCase verificationCase = requireCase(caseId);
        requireCanViewCase(actor, verificationCase);
        return toCaseResponse(verificationCase);
    }

    @Transactional(readOnly = true)
    public DocumentListResponse listDocuments(Actor actor, String caseId) {
        LegalVerificationCase verificationCase = requireCase(caseId);
        requireCanViewCase(actor, verificationCase);
        return new DocumentListResponse(documentRepository
                .findByLegalVerificationCase_CaseIdOrderByUploadedAtAsc(caseId)
                .stream().map(this::toDocumentResponse).toList());
    }

    public CaseDocumentResponse uploadDocument(Actor actor, String caseId, DocumentType documentType,
                                               MultipartFile file) {
        LegalVerificationCase verificationCase = requireCase(caseId);
        requireExecutorOwner(actor, verificationCase);
        if (verificationCase.getStatus() != CaseStatus.DRAFT
                && verificationCase.getStatus() != CaseStatus.NEEDS_INFORMATION) {
            throw Flow5Exception.conflict("Documents can only be added while the case is DRAFT or NEEDS_INFORMATION.");
        }
        if (file == null || file.isEmpty()) {
            throw new Flow5Exception(HttpStatus.BAD_REQUEST, "EMPTY_FILE", "Choose a non-empty document.");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new Flow5Exception(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", "Each document must be 10 MB or smaller.");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException exception) {
            throw new Flow5Exception(HttpStatus.BAD_REQUEST, "FILE_READ_FAILED", "The uploaded document could not be read.");
        }
        String mediaType = detectMediaType(bytes);
        String documentId = UUID.randomUUID().toString();
        DocumentStorage.StoredDocument stored = documentStorage.store(documentId, bytes);
        try {
            LegalVerificationDocument document = new LegalVerificationDocument(
                    documentId,
                    verificationCase,
                    documentType,
                    stored.storageKey(),
                    sanitizeFileName(file.getOriginalFilename()),
                    mediaType,
                    bytes.length,
                    stored.sha256(),
                    actor.userId());
            documentRepository.save(document);
            recordEvent(verificationCase, actor, CaseEventType.DOCUMENT_UPLOADED,
                    verificationCase.getStatus(), verificationCase.getStatus(), null,
                    Map.of("documentType", documentType.name()));
            return toDocumentResponse(document);
        } catch (RuntimeException failure) {
            try {
                documentStorage.delete(stored.storageKey());
            } catch (RuntimeException cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }
    }

    @Transactional(readOnly = true)
    public StoredContent readDocument(Actor actor, String caseId, String documentId) {
        LegalVerificationCase verificationCase = requireCase(caseId);
        requireCanReadContent(actor, verificationCase);
        LegalVerificationDocument document = documentRepository
                .findByDocumentIdAndLegalVerificationCase_CaseId(documentId, caseId)
                .orElseThrow(Flow5Exception::notFound);
        return new StoredContent(document.getDisplayFilename(), document.getMediaType(),
                documentStorage.read(document.getStorageKey()));
    }

    public LegalVerificationCaseResponse submit(Actor actor, String caseId) {
        LegalVerificationCase verificationCase = requireCase(caseId);
        requireExecutorOwner(actor, verificationCase);
        CaseStatus priorStatus = verificationCase.getStatus();
        if (priorStatus != CaseStatus.DRAFT && priorStatus != CaseStatus.NEEDS_INFORMATION) {
            throw Flow5Exception.conflict("Only DRAFT or NEEDS_INFORMATION cases can be submitted.");
        }
        if (!documentRepository.existsByLegalVerificationCase_CaseIdAndDocumentType(
                caseId, DocumentType.DEATH_CERTIFICATE)) {
            throw Flow5Exception.invalid("REQUIRED_EVIDENCE_MISSING", "A death-certificate document is required before submission.");
        }

        verificationCase.setStatus(CaseStatus.SUBMITTED);
        verificationCase.setSubmittedAt(Instant.now());
        caseRepository.save(verificationCase);
        CaseEventType eventType = priorStatus == CaseStatus.NEEDS_INFORMATION
                ? CaseEventType.CASE_RESUBMITTED : CaseEventType.CASE_SUBMITTED;
        recordEvent(verificationCase, actor, eventType, priorStatus, CaseStatus.SUBMITTED, null, null);
        return toCaseResponse(verificationCase);
    }

    public LegalVerificationCaseResponse startReview(Actor actor, String caseId) {
        requireVerifier(actor);
        LegalVerificationCase verificationCase = requireCase(caseId);
        if (verificationCase.getExecutorUserId().equals(actor.userId())) {
            throw Flow5Exception.forbidden();
        }
        if (verificationCase.getStatus() != CaseStatus.SUBMITTED) {
            throw Flow5Exception.conflict("Only SUBMITTED cases can enter review.");
        }
        if (verificationCase.getReviewerUserId() != null
                && !verificationCase.getReviewerUserId().equals(actor.userId())) {
            throw Flow5Exception.conflict("This case is already assigned to another reviewer.");
        }

        CaseStatus priorStatus = verificationCase.getStatus();
        verificationCase.setReviewerUserId(actor.userId());
        verificationCase.setStatus(CaseStatus.IN_REVIEW);
        caseRepository.save(verificationCase);
        recordEvent(verificationCase, actor, CaseEventType.REVIEW_STARTED,
                priorStatus, CaseStatus.IN_REVIEW, null, null);
        return toCaseResponse(verificationCase);
    }

    public LegalVerificationCaseResponse requestInformation(Actor actor, String caseId,
                                                           RequestInformationRequest request) {
        LegalVerificationCase verificationCase = requireCase(caseId);
        requireAssignedReviewer(actor, verificationCase);
        if (verificationCase.getStatus() != CaseStatus.IN_REVIEW) {
            throw Flow5Exception.conflict("More information can only be requested during IN_REVIEW.");
        }

        CaseStatus priorStatus = verificationCase.getStatus();
        verificationCase.setStatus(CaseStatus.NEEDS_INFORMATION);
        caseRepository.save(verificationCase);
        Map<String, Object> metadata = toMetadata(request.requestedDocumentTypes());
        recordEvent(verificationCase, actor, CaseEventType.INFORMATION_REQUESTED,
                priorStatus, CaseStatus.NEEDS_INFORMATION, request.reason().trim(), metadata);
        return toCaseResponse(verificationCase);
    }

    public LegalVerificationCaseResponse decide(Actor actor, String caseId, DecisionRequest request) {
        LegalVerificationCase verificationCase = requireCase(caseId);
        requireAssignedReviewer(actor, verificationCase);
        if (verificationCase.getStatus() != CaseStatus.IN_REVIEW) {
            throw Flow5Exception.conflict("A decision can only be recorded during IN_REVIEW.");
        }

        CaseStatus priorStatus = verificationCase.getStatus();
        CaseStatus finalStatus = request.outcome() == DecisionOutcome.APPROVED
                ? CaseStatus.APPROVED : CaseStatus.REJECTED;
        verificationCase.setStatus(finalStatus);
        verificationCase.setDecisionReason(request.reason().trim());
        verificationCase.setDecisionAttestationType(AttestationType.IN_APP_REVIEW_ACKNOWLEDGEMENT);
        verificationCase.setDecidedAt(Instant.now());
        caseRepository.save(verificationCase);
        CaseEventType eventType = finalStatus == CaseStatus.APPROVED
                ? CaseEventType.CASE_APPROVED : CaseEventType.CASE_REJECTED;
        recordEvent(verificationCase, actor, eventType, priorStatus, finalStatus,
                request.reason().trim(), null);
        return toCaseResponse(verificationCase);
    }

    @Transactional(readOnly = true)
    public CaseEventListResponse listEvents(Actor actor, String caseId) {
        LegalVerificationCase verificationCase = requireCase(caseId);
        requireCanViewCase(actor, verificationCase);
        List<CaseEventResponse> events = eventRepository
                .findByLegalVerificationCase_CaseIdOrderByOccurredAtAsc(caseId)
                .stream()
                .map(event -> new CaseEventResponse(event.getEventId(), event.getEventType(),
                        event.getActorUserId(), event.getFromStatus(), event.getToStatus(),
                        event.getReason(), event.getOccurredAt()))
                .toList();
        return new CaseEventListResponse(events);
    }

    private void recordEvent(LegalVerificationCase verificationCase, Actor actor, CaseEventType type,
                             CaseStatus from, CaseStatus to, String reason, Map<String, Object> metadata) {
        eventRepository.save(new LegalVerificationEvent(UUID.randomUUID().toString(), verificationCase,
                actor.userId(), type, from, to, reason, metadata));
    }

    private LegalVerificationCase requireCase(String caseId) {
        return caseRepository.findById(caseId).orElseThrow(Flow5Exception::notFound);
    }

    private void requireExecutor(Actor actor) {
        if (actor == null) throw Flow5Exception.unauthenticated();
    }

    private void requireVerifier(Actor actor) {
        if (actor == null || !actor.legalVerifier()) throw Flow5Exception.forbidden();
    }

    private void requireAnyRole(Actor actor) {
        if (actor == null) throw Flow5Exception.unauthenticated();
    }

    private void requireExecutorOwner(Actor actor, LegalVerificationCase verificationCase) {
        requireExecutor(actor);
        if (!verificationCase.getExecutorUserId().equals(actor.userId())) throw Flow5Exception.forbidden();
    }

    private void requireCanViewCase(Actor actor, LegalVerificationCase verificationCase) {
        requireAnyRole(actor);
        boolean owner = verificationCase.getExecutorUserId().equals(actor.userId());
        boolean assignedReviewer = actor.legalVerifier()
                && actor.userId().equals(verificationCase.getReviewerUserId());
        if (!owner && !assignedReviewer) throw Flow5Exception.forbidden();
    }

    private void requireCanReadContent(Actor actor, LegalVerificationCase verificationCase) {
        requireAnyRole(actor);
        boolean owner = verificationCase.getExecutorUserId().equals(actor.userId());
        boolean assignedReviewer = actor.legalVerifier() && actor.userId().equals(verificationCase.getReviewerUserId());
        if (!owner && !assignedReviewer) throw Flow5Exception.forbidden();
    }

    private void requireAssignedReviewer(Actor actor, LegalVerificationCase verificationCase) {
        requireVerifier(actor);
        if (verificationCase.getExecutorUserId().equals(actor.userId())
                || !actor.userId().equals(verificationCase.getReviewerUserId())) {
            throw Flow5Exception.forbidden();
        }
    }

    private LegalVerificationCaseResponse toCaseResponse(LegalVerificationCase verificationCase) {
        List<CaseDocumentResponse> documents = documentRepository
                .findByLegalVerificationCase_CaseIdOrderByUploadedAtAsc(verificationCase.getCaseId())
                .stream().map(this::toDocumentResponse).toList();
        CaseDecisionResponse decision = null;
        if (verificationCase.getStatus() == CaseStatus.APPROVED || verificationCase.getStatus() == CaseStatus.REJECTED) {
            decision = new CaseDecisionResponse(DecisionOutcome.valueOf(verificationCase.getStatus().name()),
                    verificationCase.getDecisionReason(), verificationCase.getReviewerUserId(),
                    verificationCase.getDecidedAt(), verificationCase.getDecisionAttestationType().name());
        }
        return new LegalVerificationCaseResponse(verificationCase.getCaseId(), verificationCase.getHandoverCaseId(),
                verificationCase.getAssignmentId(), verificationCase.getBeneficiaryContactId(),
                verificationCase.getBeneficiaryName(), verificationCase.getExecutorUserId(), verificationCase.getReviewerUserId(),
                verificationCase.getStatus(), verificationCase.getSubmittedAt(), verificationCase.getCreatedAt(),
                verificationCase.getUpdatedAt(), decision, documents);
    }

    private CaseSummaryResponse toSummary(LegalVerificationCase verificationCase) {
        return new CaseSummaryResponse(verificationCase.getCaseId(), verificationCase.getHandoverCaseId(),
                verificationCase.getAssignmentId(), verificationCase.getBeneficiaryContactId(),
                verificationCase.getBeneficiaryName(),
                verificationCase.getExecutorUserId(), verificationCase.getReviewerUserId(),
                verificationCase.getStatus(), verificationCase.getSubmittedAt(), verificationCase.getCreatedAt(),
                verificationCase.getUpdatedAt());
    }

    private CaseDocumentResponse toDocumentResponse(LegalVerificationDocument document) {
        return new CaseDocumentResponse(document.getDocumentId(), document.getDocumentType(),
                document.getDisplayFilename(), document.getMediaType(), document.getFileSizeBytes(),
                document.getUploadedByUserId(), document.getUploadedAt());
    }

    private Map<String, Object> toMetadata(List<DocumentType> requestedTypes) {
        if (requestedTypes == null || requestedTypes.isEmpty()) return null;
        return Map.of("requestedDocumentTypes", requestedTypes);
    }

    private String detectMediaType(byte[] bytes) {
        if (startsWith(bytes, "%PDF-".getBytes(StandardCharsets.US_ASCII))) return "application/pdf";
        if (bytes.length >= 3 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8
                && (bytes[2] & 0xff) == 0xff) return "image/jpeg";
        byte[] png = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
        if (startsWith(bytes, png)) return "image/png";
        throw new Flow5Exception(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_FILE_TYPE",
                "Only PDF, JPEG, and PNG documents are accepted.");
    }

    private boolean startsWith(byte[] value, byte[] prefix) {
        if (value.length < prefix.length) return false;
        for (int index = 0; index < prefix.length; index++) {
            if (value[index] != prefix[index]) return false;
        }
        return true;
    }

    private String sanitizeFileName(String originalName) {
        String value = originalName == null ? "document" : originalName.replace('\\', '/');
        int lastSlash = value.lastIndexOf('/');
        if (lastSlash >= 0) value = value.substring(lastSlash + 1);
        value = value.replaceAll("[\\p{Cntrl}]", "_").trim();
        if (value.isBlank()) value = "document";
        return value.length() > 255 ? value.substring(value.length() - 255) : value;
    }
}
