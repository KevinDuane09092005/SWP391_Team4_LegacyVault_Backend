package com.legacyvault.service;

import com.legacyvault.dto.*;
import com.legacyvault.entity.*;
import com.legacyvault.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LegalVerificationService {

    private static final Logger logger = LoggerFactory.getLogger(LegalVerificationService.class);
    private static final DateTimeFormatter UI_DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy",
            Locale.ENGLISH);
    private static final Set<String> REJECTION_REASON_CODES = Set.of(
            "INSUFFICIENT_INFORMATION",
            "INCORRECT_INFORMATION",
            "BLURRY_UNREADABLE",
            "INVALID_DOCUMENT",
            "INSUFFICIENT_AUTHORITY",
            "SUSPECTED_FRAUD");

    private final HandoverCaseRepository handoverCaseRepository;
    private final VerificationDocumentRepository verificationDocumentRepository;
    private final VerificationReviewRepository verificationReviewRepository;
    private final UserRepository userRepository;
    private final ContactRepository contactRepository;
    private final EmailService emailService;
    private final FileStorageService fileStorageService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    public LegalVerificationService(HandoverCaseRepository handoverCaseRepository,
            VerificationDocumentRepository verificationDocumentRepository,
            VerificationReviewRepository verificationReviewRepository,
            UserRepository userRepository,
            ContactRepository contactRepository,
            EmailService emailService,
            FileStorageService fileStorageService,
            AuditLogService auditLogService,
            NotificationService notificationService) {
        this.handoverCaseRepository = handoverCaseRepository;
        this.verificationDocumentRepository = verificationDocumentRepository;
        this.verificationReviewRepository = verificationReviewRepository;
        this.userRepository = userRepository;
        this.contactRepository = contactRepository;
        this.emailService = emailService;
        this.fileStorageService = fileStorageService;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public Page<VerificationQueueItemResponse> getVerificationQueue(String status, String search, Pageable pageable) {
        String filterStatus = (status != null && !status.equalsIgnoreCase("ALL")) ? status.toUpperCase() : null;
        String searchTerm = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        Page<HandoverCase> casesPage = handoverCaseRepository.searchVerificationQueue(filterStatus, searchTerm,
                pageable);

        List<VerificationQueueItemResponse> items = casesPage.getContent().stream().map(c -> {
            String ownerName = "Unknown";
            Optional<User> ownerOpt = userRepository.findById(c.getOwnerId());
            if (ownerOpt.isPresent()) {
                ownerName = ownerOpt.get().getFullName();
            }

            String executorName = "Unknown Executor";
            Optional<Contact> executorOpt = findPrimaryExecutor(c.getOwnerId());
            if (executorOpt.isPresent()) {
                executorName = executorOpt.get().getFullName();
            }

            LocalDateTime dateToFormat = c.getSubmittedAt() != null ? c.getSubmittedAt() : c.getTriggeredAt();
            String submittedDate = dateToFormat != null ? dateToFormat.toLocalDate().toString() : "";
            String submittedDateFormatted = dateToFormat != null ? dateToFormat.format(UI_DATE_FORMATTER) : "Pending";

            long docCount = verificationDocumentRepository.countByCaseId(c.getCaseId());

            return new VerificationQueueItemResponse(
                    c.getCaseId(),
                    c.getCaseNumber(),
                    ownerName,
                    executorName,
                    submittedDate,
                    submittedDateFormatted,
                    c.getStatus(),
                    formatStatusLabel(c.getStatus()),
                    (int) docCount);
        }).collect(Collectors.toList());

        return new PageImpl<>(items, pageable, casesPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public VerificationCaseDetailResponse getCaseDetail(String caseIdOrNumber) {
        HandoverCase c = findCase(caseIdOrNumber);

        VerificationCaseDetailResponse response = new VerificationCaseDetailResponse();
        response.setCaseId(c.getCaseId());
        response.setCaseNumber(c.getCaseNumber());
        response.setStatus(c.getStatus());
        response.setStatusLabel(formatStatusLabel(c.getStatus()));
        response.setRejectionReason(c.getRejectionReason());

        LocalDateTime dateToFormat = c.getSubmittedAt() != null ? c.getSubmittedAt() : c.getTriggeredAt();
        response.setSubmittedDateFormatted(dateToFormat != null ? dateToFormat.format(UI_DATE_FORMATTER) : "Pending");
        response.setReviewGuideline(
                "Confirm the document is authentic and matches the vault owner's legal record before approving this case.");

        userRepository.findById(c.getOwnerId()).ifPresent(owner -> {
            response.setVaultOwnerName(owner.getFullName());
            response.setVaultOwnerEmail(owner.getEmail());
        });

        findPrimaryExecutor(c.getOwnerId()).ifPresent(executor -> {
            response.setExecutorName(executor.getFullName());
            response.setExecutorEmail(executor.getEmail());
        });

        if (c.getAssignedVerifierId() != null) {
            userRepository.findById(c.getAssignedVerifierId()).ifPresent(verifier -> {
                response.setAssignedVerifierName(verifier.getFullName());
            });
        }

        List<VerificationDocument> docs = verificationDocumentRepository
                .findByCaseIdOrderByUploadedAtDesc(c.getCaseId());
        List<VerificationDocumentResponse> docResponses = docs.stream().map(d -> {
            VerificationDocumentResponse dr = new VerificationDocumentResponse();
            dr.setDocumentId(d.getDocumentId());
            dr.setDocumentType(d.getDocumentType());
            dr.setDocumentNumber(d.getDocumentNumber());
            dr.setIssuedBy(d.getIssuedBy());
            dr.setIssuedDate(d.getIssuedDate() != null ? d.getIssuedDate().toString() : null);
            dr.setFileName(d.getFileName());
            dr.setFileSizeBytes(d.getFileSizeBytes());
            dr.setFileSizeFormatted(formatFileSize(d.getFileSizeBytes()));
            dr.setUploadedBy("Executor");
            dr.setFileUrl(d.getFileUrl());
            dr.setFileHashSha256(d.getFileHashSha256());
            dr.setUploadedAt(d.getUploadedAt());
            return dr;
        }).collect(Collectors.toList());

        response.setDocuments(docResponses);
        return response;
    }

    @Transactional
    public VerificationCaseDetailResponse startReview(String caseIdOrNumber, String verifierId) {
        HandoverCase c = findCase(caseIdOrNumber);
        ensureCaseNotFraudHeld(c);
        String resolvedVerifierId = resolveValidVerifierUserId(verifierId, c.getOwnerId());
        c.setStatus("UNDER_REVIEW");
        c.setAssignedVerifierId(resolvedVerifierId);
        handoverCaseRepository.save(c);
        logger.info("Case {} review started by verifierId={}", c.getCaseNumber(), resolvedVerifierId);
        return getCaseDetail(c.getCaseId());
    }

    @Transactional
    public ApproveCaseResponse approveCase(String caseIdOrNumber, String verifierId, ApproveCaseRequest request) {
        HandoverCase c = findCase(caseIdOrNumber);
        ensureCaseNotFraudHeld(c);
        String resolvedVerifierId = resolveValidVerifierUserId(verifierId, c.getOwnerId());

        c.setStatus("APPROVED");
        c.setAssignedVerifierId(resolvedVerifierId);
        c.setVerifiedAt(LocalDateTime.now());
        c.setLegalVerificationSummary("Certified Court Order Verified");
        c.setClearanceStatus("CLEARANCE_GRANTED");
        c.setVaultUnlockToken("vlt_unlock_" + UUID.randomUUID().toString().replace("-", ""));
        handoverCaseRepository.save(c);

        VerificationReview review = new VerificationReview();
        review.setCaseId(c.getCaseId());
        review.setVerifierId(resolvedVerifierId != null ? resolvedVerifierId : c.getOwnerId());
        review.setDecision("APPROVED");
        review.setDetailedExplanation(request != null ? request.getSignatureNote() : "Approved by Legal Verifier");
        review.setVerifierSignaturePayload(
                request != null ? request.getVerifierSignaturePayload() : "SIGNED_SHA256_VALIDATED");
        verificationReviewRepository.save(review);

        logger.info("Case {} APPROVED by verifierId={}", c.getCaseNumber(), resolvedVerifierId);

        // Gửi mail thông báo đến Executor đã xác thực — Người thụ hưởng KHÔNG nhận mail ở bước này
        Optional<Contact> executorOpt = findPrimaryExecutor(c.getOwnerId());
        if (executorOpt.isPresent()) {
            Contact executor = executorOpt.get();

            List<Contact> beneficiaryContacts = contactRepository.findByOwnerId(c.getOwnerId()).stream()
                    .filter(contact -> Boolean.TRUE.equals(contact.getIsBeneficiary())
                            && "verified".equalsIgnoreCase(contact.getStatus()))
                    .collect(Collectors.toList());

            String beneficiaryEmails = beneficiaryContacts.stream()
                    .map(Contact::getEmail)
                    .collect(Collectors.joining(", "));
            String beneficiaryNames = beneficiaryContacts.stream()
                    .map(Contact::getFullName)
                    .collect(Collectors.joining(", "));

            emailService.sendVerificationApprovedToExecutor(
                    executor.getEmail(),
                    executor.getFullName(),
                    c.getCaseNumber(),
                    beneficiaryEmails.isEmpty() ? null : beneficiaryEmails,
                    beneficiaryNames.isEmpty() ? null : beneficiaryNames);
        } else {
            logger.warn("Case {} APPROVED nhưng không tìm thấy Executor để gửi mail thông báo.", c.getCaseNumber());
        }

        return new ApproveCaseResponse(
                c.getCaseId(),
                c.getCaseNumber(),
                "APPROVED",
                "Approved",
                "Case approved",
                "Document verified successfully. Case #" + c.getCaseNumber()
                        + " was approved and vault unlock has been authorized.",
                true,
                c.getVaultUnlockToken(),
                c.getVerifiedAt());
    }

    @Transactional
    public VerificationCaseDetailResponse rejectCase(String caseIdOrNumber, String verifierId,
            RejectCaseRequest request) {
        HandoverCase c = findCase(caseIdOrNumber);
        String resolvedVerifierId = resolveValidVerifierUserId(verifierId, c.getOwnerId());
        String reason = (request != null && request.getReason() != null) ? request.getReason()
                : "Document verification failed.";
        String reasonCode = (request != null && request.getReasonCode() != null) ? request.getReasonCode()
                : "INVALID_DOCUMENT";
        reasonCode = reasonCode.trim().isEmpty()
                ? "INVALID_DOCUMENT"
                : reasonCode.trim().toUpperCase(Locale.ROOT);
        if (!REJECTION_REASON_CODES.contains(reasonCode)) {
            throw new IllegalArgumentException("Unsupported legal rejection reason: " + reasonCode);
        }
        boolean suspectedFraud = "SUSPECTED_FRAUD".equals(reasonCode);

        ensureCaseNotFraudHeld(c);

        c.setStatus("REJECTED");
        c.setAssignedVerifierId(resolvedVerifierId);
        c.setRejectionReason(reason);
        if (suspectedFraud) {
            c.setClearanceStatus("FRAUD_HOLD");
            c.setVaultUnlockToken(null);
        }
        handoverCaseRepository.save(c);

        VerificationReview review = new VerificationReview();
        review.setCaseId(c.getCaseId());
        review.setVerifierId(resolvedVerifierId != null ? resolvedVerifierId : c.getOwnerId());
        review.setDecision("REJECTED");
        review.setRejectionReasonCode(suspectedFraud ? "SUSPECTED_FRAUD" : reasonCode);
        review.setDetailedExplanation(reason);
        verificationReviewRepository.save(review);

        logger.warn("Case {} REJECTED by verifierId={}, reason={}", c.getCaseNumber(), resolvedVerifierId, reason);

        if (suspectedFraud) {
            recordFraudHold(c, resolvedVerifierId, reason);
        }

        // Gửi email thông báo từ chối đến Executor đã xác thực
        Optional<Contact> executorOpt = findPrimaryExecutor(c.getOwnerId());
        if (executorOpt.isPresent()) {
            Contact executor = executorOpt.get();
            if (suspectedFraud) {
                emailService.sendFraudHoldNoticeToExecutor(
                        executor.getEmail(), executor.getFullName(), c.getCaseNumber());
            } else {
                emailService.sendVerificationRejectedToExecutor(
                        executor.getEmail(),
                        executor.getFullName(),
                        c.getCaseNumber(),
                        reason,
                        reasonCode);
            }
        } else {
            logger.warn("Case {} REJECTED nhưng không tìm thấy Executor để gửi mail thông báo.", c.getCaseNumber());
        }

        return getCaseDetail(c.getCaseId());
    }

    @Transactional
    public VerificationDocumentResponse uploadDocument(String caseIdOrNumber,
            MultipartFile file,
            String documentType,
            String documentNumber,
            String issuedBy,
            LocalDate issuedDate) throws IOException {
        HandoverCase c = findCase(caseIdOrNumber);
        ensureCaseNotFraudHeld(c);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        StoredFileInfo storedInfo = fileStorageService.storeFile(
                file, "verification-documents/" + c.getCaseNumber());

        String documentId = UUID.randomUUID().toString();

        VerificationDocument doc = new VerificationDocument();
        doc.setDocumentId(documentId);
        doc.setCaseId(c.getCaseId());
        doc.setDocumentType(documentType != null ? documentType : "DEATH_CERTIFICATE");
        doc.setDocumentNumber(documentNumber);
        doc.setIssuedBy(issuedBy);
        doc.setIssuedDate(issuedDate);
        doc.setFileName(storedInfo.getOriginalFileName());
        doc.setFileSizeBytes(storedInfo.getSizeBytes());
        doc.setStoragePath(storedInfo.getRelativePath());
        doc.setFileUrl("/api/v1/legal-verification/documents/" + documentId + "/view");
        doc.setFileHashSha256(storedInfo.getSha256Checksum());
        doc.setUploadedAt(LocalDateTime.now());

        VerificationDocument savedDoc = verificationDocumentRepository.save(doc);

        VerificationDocumentResponse response = new VerificationDocumentResponse();
        response.setDocumentId(savedDoc.getDocumentId());
        response.setDocumentType(savedDoc.getDocumentType());
        response.setDocumentNumber(savedDoc.getDocumentNumber());
        response.setIssuedBy(savedDoc.getIssuedBy());
        response.setIssuedDate(savedDoc.getIssuedDate() != null ? savedDoc.getIssuedDate().toString() : null);
        response.setFileName(savedDoc.getFileName());
        response.setFileSizeBytes(savedDoc.getFileSizeBytes());
        response.setFileSizeFormatted(formatFileSize(savedDoc.getFileSizeBytes()));
        response.setUploadedBy("Executor");
        response.setFileUrl(savedDoc.getFileUrl());
        response.setFileHashSha256(savedDoc.getFileHashSha256());
        response.setUploadedAt(savedDoc.getUploadedAt());
        return response;
    }

    public static class DocumentResourceInfo {
        private final Resource resource;
        private final String fileName;
        private final String contentType;

        public DocumentResourceInfo(Resource resource, String fileName, String contentType) {
            this.resource = resource;
            this.fileName = fileName;
            this.contentType = contentType;
        }

        public Resource getResource() {
            return resource;
        }

        public String getFileName() {
            return fileName;
        }

        public String getContentType() {
            return contentType;
        }
    }

    @Transactional(readOnly = true)
    public DocumentResourceInfo getDocumentResource(String documentId) throws IOException {
        VerificationDocument doc = verificationDocumentRepository.findById(documentId)
                .orElseThrow(() -> new NoSuchElementException("Verification document not found: " + documentId));

        String path = doc.getStoragePath();
        if (path == null || path.isBlank()) {
            HandoverCase c = handoverCaseRepository.findById(doc.getCaseId()).orElse(null);
            String caseNum = c != null ? c.getCaseNumber() : "unknown";
            path = "verification-documents/" + caseNum + "/" + doc.getFileName();
        }

        Resource resource = fileStorageService.loadFileAsResource(path);
        String contentType = determineContentType(doc.getFileName());

        return new DocumentResourceInfo(resource, doc.getFileName(), contentType);
    }

    private String determineContentType(String fileName) {
        if (fileName == null) {
            return "application/octet-stream";
        }
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf")) {
            return "application/pdf";
        } else if (lower.endsWith(".png")) {
            return "image/png";
        } else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        return "application/octet-stream";
    }

    @Transactional
    public VerificationCaseDetailResponse submitVerificationRequest(String caseIdOrNumber, String notes) {
        HandoverCase c = findCase(caseIdOrNumber);
        ensureCaseNotFraudHeld(c);
        if (!verificationDocumentRepository.existsByCaseIdAndDocumentType(c.getCaseId(), "DEATH_CERTIFICATE")
                && verificationDocumentRepository.countByCaseId(c.getCaseId()) == 0) {
            throw new IllegalStateException("Cannot submit verification request without death certificate document");
        }

        c.setStatus("SUBMITTED");
        c.setSubmittedAt(LocalDateTime.now());
        handoverCaseRepository.save(c);
        logger.info("Case {} submitted for legal verification by Executor", c.getCaseNumber());
        return getCaseDetail(c.getCaseId());
    }

    /**
     * Ưu tiên chọn Executor đã xác thực (status = 'verified'), nếu không có mới lấy Executor đầu tiên.
     */
    private Optional<Contact> findPrimaryExecutor(String ownerId) {
        List<Contact> executors = contactRepository.findByOwnerIdAndIsExecutorTrue(ownerId);
        if (executors == null || executors.isEmpty()) {
            return Optional.empty();
        }
        return executors.stream()
                .filter(c -> "verified".equalsIgnoreCase(c.getStatus()))
                .findFirst()
                .or(() -> Optional.of(executors.get(0)));
    }

    /**
     * Đảm bảo verifierId luôn tương thích với khóa ngoại fk_handover_verifier và fk_review_verifier -> users(user_id).
     */
    private String resolveValidVerifierUserId(String verifierId, String fallbackOwnerId) {
        boolean isPlaceholder = verifierId == null
                || verifierId.isBlank()
                || "verifier-demo".equalsIgnoreCase(verifierId)
                || "system-verifier".equalsIgnoreCase(verifierId);

        if (!isPlaceholder) {
            if (verifierId.contains("@")) {
                Optional<User> byEmail = userRepository.findByEmail(verifierId.trim());
                if (byEmail.isPresent()) {
                    return byEmail.get().getId();
                }
            }
            return verifierId;
        }

        List<User> allUsers = userRepository.findAll();
        if (allUsers != null && !allUsers.isEmpty()) {
            return allUsers.stream()
                    .filter(u -> "LEGAL_VERIFIER".equalsIgnoreCase(u.getRole())
                            || "SYSTEM_ADMIN".equalsIgnoreCase(u.getRole()))
                    .map(User::getId)
                    .findFirst()
                    .orElse(fallbackOwnerId != null ? fallbackOwnerId : allUsers.get(0).getId());
        }

        return fallbackOwnerId != null ? fallbackOwnerId : "system-verifier";
    }

    private void ensureCaseNotFraudHeld(HandoverCase handoverCase) {
        if (handoverCase != null && "FRAUD_HOLD".equalsIgnoreCase(handoverCase.getClearanceStatus())) {
            throw new IllegalStateException(
                    "This case is on hold for suspected fraud and cannot be resubmitted or approved.");
        }
    }

    private void recordFraudHold(HandoverCase handoverCase, String verifierId, String reason) {
        User verifier = verifierId != null ? userRepository.findById(verifierId).orElse(null) : null;
        String verifierName = verifier != null && verifier.getFullName() != null
                ? verifier.getFullName() : "Legal Verifier";
        String verifierRole = verifier != null && verifier.getRole() != null
                ? verifier.getRole() : "LEGAL_VERIFIER";

        auditLogService.recordLog(
                verifierId,
                verifierName,
                verifierRole,
                "LEGAL_FRAUD_HOLD",
                handoverCase.getCaseNumber(),
                "SUCCESS",
                null,
                "Reason code: SUSPECTED_FRAUD; Verifier note: " + reason);

        Map<String, User> recipients = new LinkedHashMap<>();
        User owner = handoverCase.getOwnerId() != null
                ? userRepository.findById(handoverCase.getOwnerId()).orElse(null) : null;
        if (owner != null && owner.getId() != null) {
            recipients.put(owner.getId(), owner);
        }

        List<User> allUsers = userRepository.findAll();
        if (allUsers != null) {
            for (User user : allUsers) {
                if (user == null || user.getId() == null || !isActiveUser(user) || !isAdminRole(user.getRole())) {
                    continue;
                }
                recipients.put(user.getId(), user);
            }
        }

        for (User recipient : recipients.values()) {
            boolean admin = isAdminRole(recipient.getRole());
            String title = admin ? "Legal case flagged for review" : "Legal case review on hold";
            String content = admin
                    ? "Case #" + handoverCase.getCaseNumber()
                            + " was flagged for suspected fraud. Asset release is on hold pending investigation."
                    : "Case #" + handoverCase.getCaseNumber()
                            + " has been placed on hold for additional legal review. Asset access is paused while the case is reviewed.";
            notificationService.createNotification(
                    recipient.getId(),
                    title,
                    content,
                    "LEGAL_FRAUD_HOLD",
                    admin ? "Urgent" : "Review",
                    admin ? "/dashboard/admin/audit" : "/dashboard");
        }
    }

    private boolean isAdminRole(String role) {
        if (role == null) {
            return false;
        }
        String normalizedRole = role.trim().toUpperCase(Locale.ROOT);
        return "ADMIN".equals(normalizedRole) || "SYSTEM_ADMIN".equals(normalizedRole);
    }

    private boolean isActiveUser(User user) {
        String status = user.getAccountStatus();
        return status == null || status.isBlank() || "ACTIVE".equalsIgnoreCase(status.trim());
    }

    private HandoverCase findCase(String caseIdOrNumber) {
        return handoverCaseRepository.findById(caseIdOrNumber)
                .or(() -> handoverCaseRepository.findByCaseNumber(caseIdOrNumber))
                .orElseThrow(() -> new NoSuchElementException("Handover Case not found: " + caseIdOrNumber));
    }

    private String formatStatusLabel(String status) {
        if (status == null)
            return "Pending";
        switch (status.toUpperCase()) {
            case "UNDER_REVIEW":
                return "Under Review";
            case "APPROVED":
                return "Approved";
            case "REJECTED":
                return "Rejected";
            case "SUBMITTED":
                return "Submitted";
            case "INITIATED":
                return "Initiated";
            case "HANDOVER_IN_PROGRESS":
                return "Handover In Progress";
            case "COMPLETED":
                return "Completed";
            default:
                return status;
        }
    }

    private String formatFileSize(Long bytes) {
        if (bytes == null || bytes == 0)
            return "0 B";
        if (bytes < 1024)
            return bytes + " B";
        if (bytes < 1024 * 1024)
            return String.format(Locale.ENGLISH, "%.1f KB", bytes / 1024.0);
        return String.format(Locale.ENGLISH, "%.1f MB", bytes / (1024.0 * 1024.0));
    }

    @SuppressWarnings("unused")
    private String calculateSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return "UNKNOWN_HASH";
        }
    }
}
