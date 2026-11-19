package com.legacyvault.service;

import com.legacyvault.dto.AssetResponse;
import com.legacyvault.dto.CreateAssetRequest;
import com.legacyvault.dto.StoredFileInfo;
import com.legacyvault.dto.UpdateAssetRequest;
import com.legacyvault.entity.Asset;
import com.legacyvault.entity.AssetAssignment;
import com.legacyvault.entity.AssignmentBeneficiary;
import com.legacyvault.entity.BeneficiaryClaimSession;
import com.legacyvault.entity.Contact;
import com.legacyvault.entity.HandoverCase;
import com.legacyvault.entity.HandoverReceipt;
import com.legacyvault.entity.User;
import com.legacyvault.repository.AssetAssignmentRepository;
import com.legacyvault.repository.AssetRepository;
import com.legacyvault.repository.AssignmentBeneficiaryRepository;
import com.legacyvault.repository.BeneficiaryClaimSessionRepository;
import com.legacyvault.repository.ContactRepository;
import com.legacyvault.repository.HandoverCaseRepository;
import com.legacyvault.repository.HandoverReceiptRepository;
import com.legacyvault.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AssetService {

    private static final Logger logger = LoggerFactory.getLogger(AssetService.class);
    private static final String ASSET_DOCUMENTS_DIR = "asset-documents";

    private final AssetRepository assetRepository;
    private final EncryptionService encryptionService;
    private final FileStorageService fileStorageService;
    private final AssetAssignmentRepository assetAssignmentRepository;
    private final AssignmentBeneficiaryRepository assignmentBeneficiaryRepository;
    private final HandoverCaseRepository handoverCaseRepository;
    private final BeneficiaryClaimSessionRepository claimSessionRepository;
    private final HandoverReceiptRepository handoverReceiptRepository;
    private final UserRepository userRepository;
    private final ContactRepository contactRepository;

    public AssetService(AssetRepository assetRepository, EncryptionService encryptionService) {
        this(assetRepository, encryptionService, null, null, null, null, null, null, null, null);
    }

    @Autowired
    public AssetService(
            AssetRepository assetRepository,
            EncryptionService encryptionService,
            FileStorageService fileStorageService,
            AssetAssignmentRepository assetAssignmentRepository,
            AssignmentBeneficiaryRepository assignmentBeneficiaryRepository,
            HandoverCaseRepository handoverCaseRepository,
            BeneficiaryClaimSessionRepository claimSessionRepository,
            HandoverReceiptRepository handoverReceiptRepository,
            UserRepository userRepository,
            ContactRepository contactRepository) {
        this.assetRepository = assetRepository;
        this.encryptionService = encryptionService;
        this.fileStorageService = fileStorageService;
        this.assetAssignmentRepository = assetAssignmentRepository;
        this.assignmentBeneficiaryRepository = assignmentBeneficiaryRepository;
        this.handoverCaseRepository = handoverCaseRepository;
        this.claimSessionRepository = claimSessionRepository;
        this.handoverReceiptRepository = handoverReceiptRepository;
        this.userRepository = userRepository;
        this.contactRepository = contactRepository;
    }

    @Transactional
    public AssetResponse createAsset(String ownerId, CreateAssetRequest request) {
        if (ownerId == null || ownerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Owner ID is required");
        }

        String encryptedCredentials = null;
        if (request.getCredentials() != null && !request.getCredentials().trim().isEmpty()) {
            encryptedCredentials = encryptionService.encrypt(request.getCredentials());
        }

        Asset asset = new Asset(
                ownerId,
                request.getAssetType(),
                request.getAssetName(),
                request.getDescription(),
                encryptedCredentials,
                request.getAttachmentUrl()
        );

        Asset savedAsset = assetRepository.save(asset);
        return mapToResponse(savedAsset, false);
    }

    @Transactional(readOnly = true)
    public List<AssetResponse> getAssetsByOwner(String ownerId) {
        return getAssetsByOwner(ownerId, null, null);
    }

    @Transactional(readOnly = true)
    public List<AssetResponse> getAssetsByOwner(String ownerId, String status, String assetType) {
        if (ownerId == null || ownerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Owner ID is required");
        }
        return assetRepository.findByOwnerId(ownerId)
                .stream()
                .filter(asset -> {
                    if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
                        return true;
                    }
                    return asset.getStatus() != null && asset.getStatus().equalsIgnoreCase(status.trim());
                })
                .filter(asset -> {
                    if (assetType == null || assetType.isBlank() || "ALL".equalsIgnoreCase(assetType)) {
                        return true;
                    }
                    String target = assetType.trim().replace("_", " ");
                    String current = asset.getAssetType() != null ? asset.getAssetType().trim().replace("_", " ") : "";
                    return current.equalsIgnoreCase(target);
                })
                .map(asset -> mapToResponse(asset, false))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AssetResponse getAssetDetail(String ownerId, String assetId) {
        Asset asset = assetRepository.findByAssetIdAndOwnerId(assetId, ownerId)
                .orElseThrow(() -> new RuntimeException("Asset not found or access denied"));

        String decryptedCredentials = null;
        if (asset.getEncryptedCredentials() != null) {
            decryptedCredentials = encryptionService.decrypt(asset.getEncryptedCredentials());
        }

        AssetResponse response = mapToResponse(asset, true);
        response.setDecryptedCredentials(decryptedCredentials);
        return response;
    }

    @Transactional
    public AssetResponse updateAsset(String ownerId, String assetId, UpdateAssetRequest request) {
        if (ownerId == null || ownerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Owner ID is required");
        }
        if (assetId == null || assetId.trim().isEmpty()) {
            throw new IllegalArgumentException("Asset ID is required");
        }
        if (request == null) {
            throw new IllegalArgumentException("Request body cannot be null");
        }

        Asset asset = assetRepository.findByAssetIdAndOwnerId(assetId, ownerId)
                .orElseThrow(() -> new RuntimeException("Asset not found or access denied"));

        if (request.getAssetName() != null && !request.getAssetName().trim().isEmpty()) {
            asset.setAssetName(request.getAssetName().trim());
        }

        if (request.getAssetType() != null && !request.getAssetType().trim().isEmpty()) {
            asset.setAssetType(request.getAssetType().trim());
        }

        asset.setDescription(request.getDescription());

        // Update encryptedCredentials only if new credentials are provided
        if (request.getCredentials() != null && !request.getCredentials().trim().isEmpty()) {
            asset.setEncryptedCredentials(encryptionService.encrypt(request.getCredentials()));
        }

        String previousAttachmentUrl = asset.getAttachmentUrl();
        if (request.getAttachmentUrl() != null) {
            asset.setAttachmentUrl(request.getAttachmentUrl());
        }

        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            asset.setStatus(request.getStatus().trim());
        }

        asset.setUpdatedAt(LocalDateTime.now());
        Asset savedAsset = assetRepository.save(asset);

        if (request.getAttachmentUrl() != null) {
            Set<String> oldFiles = extractStoredDocumentFileNames(previousAttachmentUrl);
            Set<String> newFiles = extractStoredDocumentFileNames(request.getAttachmentUrl());
            for (String oldFile : oldFiles) {
                if (!newFiles.contains(oldFile)) {
                    deleteStoredAssetDocumentQuietly(oldFile);
                }
            }
        }

        String decryptedCredentials = null;
        if (savedAsset.getEncryptedCredentials() != null) {
            decryptedCredentials = encryptionService.decrypt(savedAsset.getEncryptedCredentials());
        }

        AssetResponse response = mapToResponse(savedAsset, true);
        response.setDecryptedCredentials(decryptedCredentials);
        return response;
    }

    @Transactional
    public void deleteAsset(String ownerId, String assetId) {
        if (ownerId == null || ownerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Owner ID is required");
        }
        if (assetId == null || assetId.trim().isEmpty()) {
            throw new IllegalArgumentException("Asset ID is required");
        }

        Asset asset = assetRepository.findByAssetIdAndOwnerId(assetId.trim(), ownerId.trim())
                .orElseThrow(() -> new RuntimeException("Asset not found or access denied"));

        Set<String> attachedFiles = extractStoredDocumentFileNames(asset.getAttachmentUrl());
        assetRepository.delete(asset);

        for (String storedFile : attachedFiles) {
            deleteStoredAssetDocumentQuietly(storedFile);
        }
    }

    public StoredFileInfo storeEncryptedAssetDocument(String ownerId, MultipartFile file) {
        return storeEncryptedAssetDocument(ownerId, file, this.fileStorageService);
    }

    public StoredFileInfo storeEncryptedAssetDocument(
            String ownerId,
            MultipartFile file,
            FileStorageService fallbackStorageService) {
        if (ownerId == null || ownerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Owner ID is required");
        }
        FileStorageService storage = this.fileStorageService != null ? this.fileStorageService : fallbackStorageService;
        if (storage == null) {
            throw new IllegalStateException("File storage service is not available");
        }

        String validatedName = FileStorageSupport.cleanAndValidateFilename(file);
        String originalFileName = FileStorageSupport.stripEncryptedSuffix(validatedName);
        String originalContentType = FileStorageSupport.resolveContentType(file, originalFileName);

        try {
            byte[] plainBytes = file.getBytes();
            byte[] encryptedBytes = encryptionService.encryptBytes(plainBytes);
            String encryptedOriginalName = originalFileName + ".enc";

            MultipartFile encryptedMultipartFile = new InMemoryMultipartFile(
                    file.getName() != null ? file.getName() : "file",
                    encryptedOriginalName,
                    "application/octet-stream",
                    encryptedBytes
            );

            StoredFileInfo stored = storage.storeFile(encryptedMultipartFile, ASSET_DOCUMENTS_DIR);
            return new StoredFileInfo(
                    stored.getStoredFileName(),
                    originalFileName,
                    stored.getRelativePath(),
                    plainBytes.length,
                    originalContentType,
                    stored.getSha256Checksum()
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to read and encrypt uploaded document", e);
        }
    }

    @Transactional(readOnly = true)
    public DecryptedAssetDocument loadDecryptedAssetDocument(
            String storedFileName,
            String requesterId,
            String assetId,
            String caseRef) {
        return loadDecryptedAssetDocument(storedFileName, requesterId, assetId, caseRef, this.fileStorageService);
    }

    @Transactional(readOnly = true)
    public DecryptedAssetDocument loadDecryptedAssetDocument(
            String storedFileName,
            String requesterId,
            String assetId,
            String caseRef,
            FileStorageService fallbackStorageService) {
        if (storedFileName == null || storedFileName.isBlank()
                || storedFileName.contains("..")
                || storedFileName.contains("/")
                || storedFileName.contains("\\")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid document file name");
        }
        if (requesterId == null || requesterId.isBlank() || "anonymousUser".equalsIgnoreCase(requesterId.trim())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required to access asset documents");
        }

        FileStorageService storage = this.fileStorageService != null ? this.fileStorageService : fallbackStorageService;
        if (storage == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File storage service is not available");
        }

        String normalizedFileName = storedFileName.trim();
        List<Asset> matchingAssets = findAssetsReferencingDocument(normalizedFileName, assetId);
        if (matchingAssets.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset document not found");
        }

        boolean authorized = matchingAssets.stream()
                .anyMatch(asset -> isAuthorizedToAccessAssetDocument(asset, requesterId.trim(), caseRef));
        if (!authorized) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Access denied: You are not authorized to decrypt or view this asset document");
        }

        try {
            Resource resource = storage.loadFileAsResource(ASSET_DOCUMENTS_DIR + "/" + normalizedFileName);
            byte[] storedBytes;
            try (InputStream in = resource.getInputStream()) {
                storedBytes = in.readAllBytes();
            }

            byte[] plainBytes = normalizedFileName.toLowerCase(Locale.ROOT).endsWith(".enc")
                    ? encryptionService.decryptBytes(storedBytes)
                    : storedBytes;

            String displayFileName = extractDisplayFileName(normalizedFileName);
            String contentType = FileStorageSupport.inferContentType(displayFileName);
            return new DecryptedAssetDocument(displayFileName, contentType, plainBytes);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset document could not be loaded", e);
        }
    }

    private List<Asset> findAssetsReferencingDocument(String storedFileName, String assetId) {
        List<Asset> matches = new ArrayList<>();
        if (assetId != null && !assetId.isBlank()) {
            assetRepository.findById(assetId.trim()).ifPresent(asset -> {
                if (extractStoredDocumentFileNames(asset.getAttachmentUrl()).contains(storedFileName)) {
                    matches.add(asset);
                }
            });
        }
        if (matches.isEmpty()) {
            List<Asset> allAssets = assetRepository.findAll();
            if (allAssets != null) {
                for (Asset asset : allAssets) {
                    if (asset != null && extractStoredDocumentFileNames(asset.getAttachmentUrl()).contains(storedFileName)) {
                        matches.add(asset);
                    }
                }
            }
        }
        return matches;
    }

    private boolean isAuthorizedToAccessAssetDocument(Asset asset, String requesterId, String caseRef) {
        if (asset == null || requesterId == null || requesterId.isBlank()) {
            return false;
        }
        // 1. Vault Owner of this Asset
        if (asset.getOwnerId() != null && asset.getOwnerId().equals(requesterId)) {
            return true;
        }

        // 2. Designated Beneficiary assigned to this specific Asset who has completed Handover Verification & Release
        if (assetAssignmentRepository == null || assignmentBeneficiaryRepository == null || asset.getAssetId() == null) {
            return false;
        }

        Optional<AssetAssignment> optAssignment = assetAssignmentRepository.findByAssetId(asset.getAssetId());
        if (optAssignment.isEmpty() || optAssignment.get().getAssignmentId() == null) {
            return false;
        }

        List<AssignmentBeneficiary> links = assignmentBeneficiaryRepository
                .findByAssignmentId(optAssignment.get().getAssignmentId());
        if (links == null || links.isEmpty()) {
            return false;
        }

        Set<String> assignedContactIds = links.stream()
                .map(AssignmentBeneficiary::getBeneficiaryContactId)
                .filter(id -> id != null && !id.isBlank())
                .collect(Collectors.toSet());
        if (assignedContactIds.isEmpty()) {
            return false;
        }

        Set<String> requesterAssignedContactIds = resolveRequesterAssignedContactIds(
                asset.getOwnerId(), requesterId, assignedContactIds);
        if (requesterAssignedContactIds.isEmpty()) {
            return false;
        }

        return hasReleasedAndVerifiedHandoverSession(asset.getOwnerId(), requesterAssignedContactIds, caseRef);
    }

    private Set<String> resolveRequesterAssignedContactIds(
            String ownerId,
            String requesterId,
            Set<String> assignedContactIds) {
        Set<String> matched = new LinkedHashSet<>();

        List<Contact> ownerContacts = (contactRepository != null && ownerId != null)
                ? contactRepository.findByOwnerId(ownerId)
                : Collections.emptyList();

        if (assignedContactIds.contains(requesterId)) {
            if (ownerContacts == null || ownerContacts.isEmpty()) {
                matched.add(requesterId);
            } else {
                boolean verifiedOwnerBeneficiary = ownerContacts.stream()
                        .anyMatch(c -> Boolean.TRUE.equals(c.getIsBeneficiary())
                                && requesterId.equals(c.getContactId()));
                if (verifiedOwnerBeneficiary) {
                    matched.add(requesterId);
                }
            }
        }

        if (userRepository != null && ownerContacts != null && !ownerContacts.isEmpty()) {
            Optional<User> optUser = userRepository.findById(requesterId);
            if (optUser.isPresent() && optUser.get().getEmail() != null && !optUser.get().getEmail().isBlank()) {
                String userEmail = optUser.get().getEmail().trim();
                for (Contact contact : ownerContacts) {
                    if (Boolean.TRUE.equals(contact.getIsBeneficiary())
                            && contact.getContactId() != null
                            && assignedContactIds.contains(contact.getContactId())
                            && contact.getEmail() != null
                            && contact.getEmail().trim().equalsIgnoreCase(userEmail)) {
                        matched.add(contact.getContactId());
                    }
                }
            }
        }

        return matched;
    }

    private boolean hasReleasedAndVerifiedHandoverSession(
            String ownerId,
            Set<String> beneficiaryContactIds,
            String caseRef) {
        if (handoverCaseRepository == null || claimSessionRepository == null || ownerId == null) {
            return false;
        }

        List<HandoverCase> candidateCases = new ArrayList<>();
        if (caseRef != null && !caseRef.isBlank()) {
            handoverCaseRepository.findByCaseNumber(caseRef.trim()).ifPresent(candidateCases::add);
            if (candidateCases.isEmpty()) {
                handoverCaseRepository.findById(caseRef.trim()).ifPresent(candidateCases::add);
            }
        }
        if (candidateCases.isEmpty()) {
            List<HandoverCase> allCases = handoverCaseRepository.findAll();
            if (allCases != null) {
                candidateCases.addAll(allCases);
            }
        }

        for (HandoverCase c : candidateCases) {
            if (c == null || !ownerId.equals(c.getOwnerId())) {
                continue;
            }
            String status = c.getStatus() != null ? c.getStatus().trim().toUpperCase(Locale.ROOT) : "";
            if (!"RELEASED".equals(status) && !"COMPLETED".equals(status)) {
                continue;
            }

            List<BeneficiaryClaimSession> sessions = claimSessionRepository.findByCaseId(c.getCaseId());
            if (sessions != null) {
                boolean verifiedInList = sessions.stream()
                        .anyMatch(s -> s != null
                                && beneficiaryContactIds.contains(s.getBeneficiaryContactId())
                                && "ACTIVE".equalsIgnoreCase(s.getTokenStatus())
                                && Boolean.TRUE.equals(s.getIdentityVerified())
                                && "VERIFIED".equalsIgnoreCase(s.getKycFacialStatus()));
                if (verifiedInList) {
                    return true;
                }
            }

            for (String contactId : beneficiaryContactIds) {
                Optional<BeneficiaryClaimSession> directSession = claimSessionRepository
                        .findByCaseIdAndBeneficiaryContactId(c.getCaseId(), contactId);
                if (directSession.isPresent()) {
                    BeneficiaryClaimSession s = directSession.get();
                    if ("ACTIVE".equalsIgnoreCase(s.getTokenStatus())
                            && Boolean.TRUE.equals(s.getIdentityVerified())
                            && "VERIFIED".equalsIgnoreCase(s.getKycFacialStatus())) {
                        return true;
                    }
                }
            }

            if ("COMPLETED".equals(status) && handoverReceiptRepository != null) {
                List<HandoverReceipt> receipts = handoverReceiptRepository.findByCaseId(c.getCaseId());
                if (receipts != null && receipts.stream()
                        .anyMatch(r -> r != null && beneficiaryContactIds.contains(r.getBeneficiaryContactId()))) {
                    return true;
                }
            }
        }

        return false;
    }

    static String extractDisplayFileName(String storedFileName) {
        int underscoreIdx = storedFileName.indexOf('_');
        String withPotentialEnc = (underscoreIdx > 0 && underscoreIdx < storedFileName.length() - 1)
                ? storedFileName.substring(underscoreIdx + 1)
                : storedFileName;
        return FileStorageSupport.stripEncryptedSuffix(withPotentialEnc);
    }

    static Set<String> extractStoredDocumentFileNames(String attachmentUrl) {
        if (attachmentUrl == null || attachmentUrl.isBlank()) {
            return Collections.emptySet();
        }
        Set<String> result = new LinkedHashSet<>();
        String[] parts = attachmentUrl.split(",");
        for (String rawPart : parts) {
            if (rawPart == null) {
                continue;
            }
            String token = rawPart.trim();
            if (token.isEmpty()) {
                continue;
            }
            int queryIdx = token.indexOf('?');
            if (queryIdx >= 0) {
                token = token.substring(0, queryIdx);
            }
            int hashIdx = token.indexOf('#');
            if (hashIdx >= 0) {
                token = token.substring(0, hashIdx);
            }

            String marker = "/api/v1/assets/documents/";
            int markerIdx = token.indexOf(marker);
            String fileName = null;
            if (markerIdx >= 0) {
                fileName = token.substring(markerIdx + marker.length()).trim();
            } else if (token.startsWith(ASSET_DOCUMENTS_DIR + "/")) {
                fileName = token.substring((ASSET_DOCUMENTS_DIR + "/").length()).trim();
            } else if (!token.startsWith("http://") && !token.startsWith("https://") && !token.contains("/")) {
                fileName = token;
            }

            if (fileName != null && !fileName.isEmpty()
                    && !fileName.contains("..")
                    && !fileName.contains("/")
                    && !fileName.contains("\\")) {
                result.add(fileName);
            }
        }
        return result;
    }

    private void deleteStoredAssetDocumentQuietly(String storedFileName) {
        if (fileStorageService == null || storedFileName == null || storedFileName.isBlank()) {
            return;
        }
        try {
            fileStorageService.deleteFile(ASSET_DOCUMENTS_DIR + "/" + storedFileName);
        } catch (Exception e) {
            logger.warn("Failed to delete asset document {}: {}", storedFileName, e.getMessage());
        }
    }

    public static class DecryptedAssetDocument {
        private final String displayFileName;
        private final String contentType;
        private final byte[] content;

        public DecryptedAssetDocument(String displayFileName, String contentType, byte[] content) {
            this.displayFileName = displayFileName;
            this.contentType = contentType;
            this.content = content;
        }

        public String getDisplayFileName() {
            return displayFileName;
        }

        public String getContentType() {
            return contentType;
        }

        public byte[] getContent() {
            return content;
        }
    }

    private static final class InMemoryMultipartFile implements MultipartFile {
        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] content;

        private InMemoryMultipartFile(String name, String originalFilename, String contentType, byte[] content) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.content = content != null ? content : new byte[0];
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return content.length == 0;
        }

        @Override
        public long getSize() {
            return content.length;
        }

        @Override
        public byte[] getBytes() {
            return content.clone();
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(File dest) throws IOException, IllegalStateException {
            Files.write(dest.toPath(), content);
        }

        @Override
        public void transferTo(Path dest) throws IOException, IllegalStateException {
            Files.write(dest, content);
        }
    }

    private AssetResponse mapToResponse(Asset asset, boolean includeDecrypted) {
        return new AssetResponse(
                asset.getAssetId(),
                asset.getOwnerId(),
                asset.getAssetType(),
                asset.getAssetName(),
                asset.getDescription(),
                null, // Credentials not decrypted by default in list summary
                asset.getAttachmentUrl(),
                asset.getStatus(),
                asset.getCreatedAt(),
                asset.getUpdatedAt()
        );
    }
}
