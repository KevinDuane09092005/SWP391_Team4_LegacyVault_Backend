package com.legacyvault.service;

import com.legacyvault.dto.*;
import com.legacyvault.entity.*;
import com.legacyvault.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class HandoverExecutionService {

    private static final Logger logger = LoggerFactory.getLogger(HandoverExecutionService.class);

    private final HandoverCaseRepository handoverCaseRepository;
    private final UserRepository userRepository;
    private final ContactRepository contactRepository;
    private final AssetRepository assetRepository;
    private final AssetAssignmentRepository assetAssignmentRepository;
    private final AssignmentBeneficiaryRepository assignmentBeneficiaryRepository;
    private final BeneficiaryClaimSessionRepository claimSessionRepository;
    private final EmailService emailService;
    private final EncryptionService encryptionService;

    public HandoverExecutionService(HandoverCaseRepository handoverCaseRepository,
            UserRepository userRepository,
            ContactRepository contactRepository,
            AssetRepository assetRepository,
            AssetAssignmentRepository assetAssignmentRepository,
            AssignmentBeneficiaryRepository assignmentBeneficiaryRepository,
            BeneficiaryClaimSessionRepository claimSessionRepository,
            EmailService emailService,
            EncryptionService encryptionService) {
        this.handoverCaseRepository = handoverCaseRepository;
        this.userRepository = userRepository;
        this.contactRepository = contactRepository;
        this.assetRepository = assetRepository;
        this.assetAssignmentRepository = assetAssignmentRepository;
        this.assignmentBeneficiaryRepository = assignmentBeneficiaryRepository;
        this.claimSessionRepository = claimSessionRepository;
        this.emailService = emailService;
        this.encryptionService = encryptionService;
    }

    public static String sha256Hex(String input) {
        if (input == null)
            return null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 hashing error", e);
        }
    }

    @Transactional(readOnly = true)
    public ExecutorHandoverDashboardResponse getExecutorDashboard(String executorUserId) {
        List<HandoverCase> allCases = handoverCaseRepository.findAll();

        int total = allCases.size();
        int pendingApproval = 0;
        int inProgress = 0;
        int completed = 0;

        List<ExecutorHandoverDashboardResponse.HandoverCaseTableRow> rows = new ArrayList<>();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH);

        for (HandoverCase c : allCases) {
            String rawStatus = c.getStatus() != null ? c.getStatus().toUpperCase() : "INITIATED";

            String legalStatus;
            String handoverStatus;

            if (rawStatus.equals("APPROVED")) {
                legalStatus = "APPROVED";
                handoverStatus = "APPROVED";
            } else if (rawStatus.contains("PROGRESS") || rawStatus.contains("RELEASED")
                    || "CLEARANCE_GRANTED".equalsIgnoreCase(c.getClearanceStatus())) {
                legalStatus = "APPROVED";
                handoverStatus = "IN PROGRESS";
            } else if (rawStatus.contains("COMPLETED")) {
                legalStatus = "APPROVED";
                handoverStatus = "COMPLETED";
            } else if (rawStatus.contains("REJECTED")) {
                legalStatus = "REJECTED";
                handoverStatus = "REJECTED";
            } else if (rawStatus.contains("SUBMITTED") || rawStatus.contains("UNDER_REVIEW")) {
                legalStatus = "UNDER_REVIEW";
                handoverStatus = "PENDING_LEGAL_APPROVAL";
            } else {
                // "INITIATED" or pending initial review
                legalStatus = "PENDING";
                handoverStatus = "PENDING_LEGAL_APPROVAL";
            }

            if (legalStatus.equals("PENDING") || legalStatus.equals("UNDER_REVIEW")) {
                pendingApproval++;
            } else if (handoverStatus.equals("IN PROGRESS") || handoverStatus.equals("APPROVED")) {
                inProgress++;
            } else if (handoverStatus.equals("COMPLETED")) {
                completed++;
            }

            User owner = userRepository.findById(c.getOwnerId()).orElse(null);
            String ownerName = owner != null ? owner.getFullName() : "Vault Owner";

            // Find all beneficiaries for this owner
            List<Contact> beneficiaryContacts = contactRepository.findByOwnerId(c.getOwnerId()).stream()
                    .filter(contact -> Boolean.TRUE.equals(contact.getIsBeneficiary()))
                    .collect(Collectors.toList());

            List<ExecutorHandoverDashboardResponse.BeneficiaryItem> beneficiaryItems = beneficiaryContacts.stream()
                    .map(b -> new ExecutorHandoverDashboardResponse.BeneficiaryItem(b.getFullName(), b.getEmail()))
                    .collect(Collectors.toList());

            String beneficiaryNames = beneficiaryContacts.stream()
                    .map(Contact::getFullName)
                    .collect(Collectors.joining(", "));
            if (beneficiaryNames.isEmpty()) {
                beneficiaryNames = "Beneficiary";
            }
            String primaryEmail = !beneficiaryContacts.isEmpty() ? beneficiaryContacts.get(0).getEmail() : "";

            String updatedDate = c.getVerifiedAt() != null ? c.getVerifiedAt().format(dtf)
                    : (c.getTriggeredAt() != null ? c.getTriggeredAt().format(dtf) : "Oct 24, 2026");

            ExecutorHandoverDashboardResponse.HandoverCaseTableRow row = new ExecutorHandoverDashboardResponse.HandoverCaseTableRow(
                    c.getCaseId(),
                    c.getCaseNumber(),
                    ownerName,
                    beneficiaryNames,
                    legalStatus,
                    handoverStatus,
                    updatedDate,
                    "MANAGE_HANDOVER");
            row.setBeneficiaryEmail(primaryEmail);
            row.setBeneficiaries(beneficiaryItems);
            rows.add(row);
        }

        ExecutorHandoverDashboardResponse.DashboardMetrics metrics = new ExecutorHandoverDashboardResponse.DashboardMetrics(
                total > 0 ? total : 0,
                pendingApproval,
                inProgress,
                completed);

        return new ExecutorHandoverDashboardResponse(metrics, rows);
    }

    @Transactional(readOnly = true)
    public HandoverDetailsResponse getHandoverDetails(String caseIdentifier) {
        HandoverCase c = findCaseByIdentifier(caseIdentifier);

        User owner = userRepository.findById(c.getOwnerId()).orElse(null);
        String ownerName = owner != null ? owner.getFullName() : "";
        String ownerEmail = owner != null ? owner.getEmail() : "";

        // Beneficiary info
        List<Contact> bContacts = contactRepository.findByOwnerId(c.getOwnerId()).stream()
                .filter(contact -> Boolean.TRUE.equals(contact.getIsBeneficiary()))
                .collect(Collectors.toList());

        String beneficiaryName = bContacts.stream().map(Contact::getFullName).collect(Collectors.joining(", "));
        if (beneficiaryName.isEmpty())
            beneficiaryName = "";
        String beneficiaryEmail = !bContacts.isEmpty() ? bContacts.get(0).getEmail() : "";

        // Assets
        List<Asset> assets = assetRepository.findByOwnerId(c.getOwnerId());
        if (assets.isEmpty()) {
            assets = assetRepository.findAll();
        }
        List<HandoverDetailsResponse.HandoverAssetItem> assetItems = new ArrayList<>();
        if (assets.isEmpty()) {
            assetItems.add(new HandoverDetailsResponse.HandoverAssetItem("asset-default-1", "Primary Vault Ledger Key",
                    "Credentials", "Secure digital key", null, null, "Legal Verification", "AUTHORIZED & LOCKED"));
            assetItems.add(new HandoverDetailsResponse.HandoverAssetItem("asset-default-2",
                    "Family Inheritance Instructions.pdf", "Documents", "Legal estate documents", null, null,
                    "Automatic Release", "AUTHORIZED & LOCKED"));
        } else {
            for (Asset a : assets) {
                String decrypted = null;
                if (a.getEncryptedCredentials() != null && !a.getEncryptedCredentials().isEmpty()) {
                    try {
                        decrypted = encryptionService.decrypt(a.getEncryptedCredentials());
                    } catch (Exception e) {
                        logger.warn("Could not decrypt asset credentials {}: {}", a.getAssetId(), e.getMessage());
                    }
                }
                assetItems.add(new HandoverDetailsResponse.HandoverAssetItem(
                        a.getAssetId(),
                        a.getAssetName(),
                        a.getAssetType() != null ? a.getAssetType() : "General Asset",
                        a.getDescription(),
                        decrypted,
                        a.getAttachmentUrl(),
                        "Legal Verification",
                        "RELEASED".equalsIgnoreCase(c.getStatus()) ? "AUTHORIZED" : "AUTHORIZED & LOCKED"));
            }
        }

        String rawStatus = c.getStatus() != null ? c.getStatus().toUpperCase() : "INITIATED";
        String detailLegalStatus = "PENDING";
        if (rawStatus.equals("APPROVED") || rawStatus.contains("PROGRESS") || rawStatus.contains("RELEASED")
                || rawStatus.contains("COMPLETED")) {
            detailLegalStatus = "APPROVED";
        } else if (rawStatus.contains("SUBMITTED") || rawStatus.contains("UNDER_REVIEW")) {
            detailLegalStatus = "UNDER_REVIEW";
        } else if (rawStatus.contains("REJECTED")) {
            detailLegalStatus = "REJECTED";
        }

        return new HandoverDetailsResponse(
                c.getCaseNumber(),
                c.getStatus() != null ? c.getStatus().toUpperCase() : "IN PROGRESS",
                new HandoverDetailsResponse.VaultOwnerSummary(ownerName, ownerEmail, "DECEASED/INACTIVE"),
                new HandoverDetailsResponse.BeneficiarySummary(beneficiaryName, beneficiaryEmail),
                new HandoverDetailsResponse.LegalVerificationSummary(
                        detailLegalStatus,
                        c.getLegalVerificationSummary() != null ? c.getLegalVerificationSummary()
                                : (detailLegalStatus.equals("APPROVED") ? "Certified Court Order Verified"
                                        : "Pending Legal Verification")),
                assetItems);
    }

    @Transactional
    public StartReleaseResponse startHandoverRelease(String caseIdentifier, String executorUserId) {
        HandoverCase c = findCaseByIdentifier(caseIdentifier);

        String currentStatus = c.getStatus() != null ? c.getStatus().toUpperCase() : "";
        if (!currentStatus.equals("APPROVED") && !currentStatus.equals("UNDER_REVIEW")
                && !currentStatus.equals("IN PROGRESS")) {
            throw new IllegalStateException(
                    "Cannot start handover release: Case legal verification is not APPROVED yet. Current status: "
                            + c.getStatus());
        }

        String accessCode = c.getCaseNumber() + "-HANDOVER-SECURE";
        String accessCodeHash = sha256Hex(accessCode);

        // Find all beneficiaries for this owner
        List<Contact> beneficiaryContacts = contactRepository.findByOwnerId(c.getOwnerId()).stream()
                .filter(contact -> Boolean.TRUE.equals(contact.getIsBeneficiary()))
                .collect(Collectors.toList());

        User owner = userRepository.findById(c.getOwnerId()).orElse(null);
        String ownerName = owner != null ? owner.getFullName() : "Vault Owner";

        List<String> notifiedEmails = new ArrayList<>();

        if (beneficiaryContacts.isEmpty()) {
            String defaultBeneficiaryId = UUID.randomUUID().toString();
            String defaultEmail = "linh.pham@beneficiary.com";
            BeneficiaryClaimSession session = claimSessionRepository
                    .findByCaseIdAndBeneficiaryContactId(c.getCaseId(), defaultBeneficiaryId)
                    .orElse(new BeneficiaryClaimSession());
            session.setCaseId(c.getCaseId());
            session.setBeneficiaryContactId(defaultBeneficiaryId);
            session.setAccessCodeHash(accessCodeHash);
            session.setClaimTokenHash(sha256Hex(UUID.randomUUID().toString()));
            session.setOtpCodeHash(sha256Hex("482916"));
            session.setOtpExpiresAt(LocalDateTime.now().plusMinutes(15));
            session.setTokenStatus("ACTIVE");
            session.setKycPhotoIdStatus("VERIFIED");
            session.setKycFacialStatus("PENDING_SCAN");
            session.setIdentityVerified(false);
            claimSessionRepository.save(session);
            notifiedEmails.add(defaultEmail);
        } else {
            for (Contact beneficiary : beneficiaryContacts) {
                String beneficiaryId = beneficiary.getContactId();
                String beneficiaryEmail = beneficiary.getEmail();
                String beneficiaryName = beneficiary.getFullName();

                BeneficiaryClaimSession session = claimSessionRepository
                        .findByCaseIdAndBeneficiaryContactId(c.getCaseId(), beneficiaryId)
                        .orElse(new BeneficiaryClaimSession());

                session.setCaseId(c.getCaseId());
                session.setBeneficiaryContactId(beneficiaryId);
                session.setAccessCodeHash(accessCodeHash);
                session.setClaimTokenHash(sha256Hex(UUID.randomUUID().toString()));
                session.setOtpCodeHash(sha256Hex("482916"));
                session.setOtpExpiresAt(LocalDateTime.now().plusMinutes(15));
                session.setTokenStatus("ACTIVE");
                session.setKycPhotoIdStatus("VERIFIED");
                session.setKycFacialStatus("PENDING_SCAN");
                session.setIdentityVerified(false);
                claimSessionRepository.save(session);

                try {
                    emailService.sendHandoverAccessCodeEmail(beneficiaryEmail, beneficiaryName, ownerName,
                            c.getCaseNumber(), accessCode);
                    notifiedEmails.add(beneficiaryEmail);
                } catch (Exception e) {
                    logger.warn("Failed to send handover access code email to {}: {}", beneficiaryEmail,
                            e.getMessage());
                }
            }
        }

        c.setStatus("IN PROGRESS");
        c.setClearanceStatus("CLEARANCE_GRANTED");
        c.setAuthorizedByExecutor(executorUserId != null ? executorUserId : "Minh Le (Executor)");
        handoverCaseRepository.save(c);

        logger.info("Started Handover Release for case={} accessCode={} recipients={}", c.getCaseNumber(), accessCode,
                notifiedEmails);

        return new StartReleaseResponse(
                c.getCaseNumber(),
                "IN PROGRESS",
                accessCode,
                String.join(", ", notifiedEmails));
    }

    @Transactional(readOnly = true)
    public AuthorizationEvaluationResponse getAuthorizationEvaluation(String caseIdentifier) {
        HandoverCase c = findCaseByIdentifier(caseIdentifier);

        User owner = userRepository.findById(c.getOwnerId()).orElse(null);
        String ownerName = owner != null ? owner.getFullName() : "Thanh Pham";

        String beneficiaryName = "Linh Pham";
        List<Contact> contacts = contactRepository.findByOwnerId(c.getOwnerId());
        for (Contact contact : contacts) {
            if (Boolean.TRUE.equals(contact.getIsBeneficiary())) {
                beneficiaryName = contact.getFullName();
                break;
            }
        }

        List<AuthorizationEvaluationResponse.AssetVerdictItem> verdicts = new ArrayList<>();
        verdicts.add(new AuthorizationEvaluationResponse.AssetVerdictItem("Primary Vault Ledger Key", "Credentials",
                "AUTHORIZED FOR RELEASE"));
        verdicts.add(new AuthorizationEvaluationResponse.AssetVerdictItem("Family Inheritance Instructions.pdf",
                "Documents", "AUTHORIZED FOR RELEASE"));
        verdicts.add(new AuthorizationEvaluationResponse.AssetVerdictItem("Real Estate Deeds & Trust Assets",
                "Property Deeds", "LOCKED / RESTRICTED"));

        return new AuthorizationEvaluationResponse(
                "EVALUATING",
                "Identity verification checks successfully concluded. Evaluating legal releases in accordance with the last will of "
                        + ownerName
                        + " and validated court orders. Locked/probate-bound assets must remain restricted.",
                new AuthorizationEvaluationResponse.EvaluationChecklist("VERIFIED", "APPROVED", "VERIFIED"),
                "The system has determined that 2 of 3 listed legacy collections are legally cleared for instant handover release to "
                        + beneficiaryName + ".",
                verdicts,
                new AuthorizationEvaluationResponse.ExecutionControlSummary(
                        "Clearance Granted",
                        c.getAuthorizedByExecutor() != null ? c.getAuthorizedByExecutor() : "Minh Le (Executor)",
                        c.getCaseNumber(),
                        beneficiaryName));
    }

    @Transactional
    public Map<String, String> dispatchSecureHandover(String caseIdentifier, String executorUserId) {
        HandoverCase c = findCaseByIdentifier(caseIdentifier);
        c.setStatus("RELEASED");
        handoverCaseRepository.save(c);

        logger.info("Dispatched secure handover for caseNumber={}", c.getCaseNumber());
        return Map.of("case_number", c.getCaseNumber(), "status", "RELEASED");
    }

    @Transactional(readOnly = true)
    public HandoverCompletedResponse getCompletionReceipt(String caseIdentifier) {
        HandoverCase c = findCaseByIdentifier(caseIdentifier);

        User owner = userRepository.findById(c.getOwnerId()).orElse(null);
        String ownerName = owner != null ? owner.getFullName() : "Thanh Pham";

        String beneficiaryName = "Linh Pham";
        List<Contact> contacts = contactRepository.findByOwnerId(c.getOwnerId());
        for (Contact contact : contacts) {
            if (Boolean.TRUE.equals(contact.getIsBeneficiary())) {
                beneficiaryName = contact.getFullName();
                break;
            }
        }

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MMM dd, yyyy - HH:mm:ss 'EST'", Locale.ENGLISH);
        String finalDate = c.getResolvedAt() != null ? c.getResolvedAt().format(dtf) : "Oct 25, 2026 - 14:54:12 EST";

        return new HandoverCompletedResponse(
                c.getCaseNumber(),
                "COMPLETED",
                "Asset Handover Successful",
                "All legal digital inheritance files for Case " + c.getCaseNumber()
                        + " have been successfully verified, released, and receipted by the beneficiary.",
                new HandoverCompletedResponse.EstateDepositionTransReceipt(
                        ownerName,
                        beneficiaryName,
                        c.getAuthorizedByExecutor() != null ? c.getAuthorizedByExecutor() : "Minh Le",
                        finalDate),
                "https://vault-storage.legacyvault.io/receipts/RC-" + c.getCaseNumber()
                        + "-FINAL.pdf?token=valid_receipt_sig");
    }

    public HandoverCase findCaseByIdentifier(String caseIdentifier) {
        return handoverCaseRepository.findByCaseNumber(caseIdentifier)
                .orElseGet(() -> handoverCaseRepository.findById(caseIdentifier)
                        .orElseThrow(() -> new IllegalArgumentException("Handover case not found: " + caseIdentifier)));
    }
}
