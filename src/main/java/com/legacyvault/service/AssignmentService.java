package com.legacyvault.service;

import com.legacyvault.dto.AssignmentResponse;
import com.legacyvault.dto.BeneficiaryDto;
import com.legacyvault.dto.CreateAssignmentRequest;
import com.legacyvault.dto.UpdateAssignmentRequest;
import java.time.LocalDateTime;
import com.legacyvault.entity.Asset;
import com.legacyvault.entity.AssetAssignment;
import com.legacyvault.entity.AssignmentBeneficiary;
import com.legacyvault.entity.Contact;
import com.legacyvault.repository.AssetAssignmentRepository;
import com.legacyvault.repository.AssetRepository;
import com.legacyvault.repository.AssignmentBeneficiaryRepository;
import com.legacyvault.repository.ContactRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AssignmentService {

    private final AssetAssignmentRepository assignmentRepository;
    private final AssignmentBeneficiaryRepository beneficiaryRepository;
    private final AssetRepository assetRepository;
    private final ContactRepository contactRepository;

    public AssignmentService(AssetAssignmentRepository assignmentRepository,
                             AssignmentBeneficiaryRepository beneficiaryRepository,
                             AssetRepository assetRepository,
                             ContactRepository contactRepository) {
        this.assignmentRepository = assignmentRepository;
        this.beneficiaryRepository = beneficiaryRepository;
        this.assetRepository = assetRepository;
        this.contactRepository = contactRepository;
    }

    public Map<String, Object> validateAssignment(String ownerId, CreateAssignmentRequest request) {
        if (ownerId == null || ownerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Owner ID is required");
        }

        List<String> distinctBeneficiaryIds = extractDistinctBeneficiaryIds(request.getBeneficiaryContactIds());
        if (distinctBeneficiaryIds.isEmpty()) {
            throw new IllegalArgumentException("At least one beneficiary contact ID is required");
        }

        // Validate Asset ownership
        Asset asset = assetRepository.findByAssetIdAndOwnerId(request.getAssetId(), ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found or access denied"));

        if (assignmentRepository.existsByAssetId(request.getAssetId())) {
            throw new IllegalStateException("Asset already has an active assignment");
        }

        // Validate Executor contact (status == verified AND isExecutor == true)
        Contact executor = contactRepository.findByContactIdAndOwnerId(request.getExecutorContactId(), ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Executor contact not found or access denied"));
        ensureContactIsVerified(executor, "Người thi hành");
        if (!Boolean.TRUE.equals(executor.getIsExecutor())) {
            throw new IllegalArgumentException("Liên hệ \"" + executor.getFullName() + "\" không có vai trò Người thi hành (Executor).");
        }

        // Validate Beneficiaries contacts (status == verified AND isBeneficiary == true)
        for (String beneficiaryId : distinctBeneficiaryIds) {
            Contact beneficiary = contactRepository.findByContactIdAndOwnerId(beneficiaryId, ownerId)
                    .orElseThrow(() -> new IllegalArgumentException("Beneficiary contact not found: " + beneficiaryId));
            ensureContactIsVerified(beneficiary, "Người thụ hưởng");
            if (!Boolean.TRUE.equals(beneficiary.getIsBeneficiary())) {
                throw new IllegalArgumentException("Liên hệ \"" + beneficiary.getFullName() + "\" không có vai trò Người thụ hưởng (Beneficiary).");
            }
        }

        return Map.of(
                "valid", true,
                "message", "Assignment configuration is valid",
                "assetName", asset.getAssetName(),
                "executorName", executor.getFullName(),
                "beneficiariesCount", distinctBeneficiaryIds.size()
        );
    }

    @Transactional
    public AssignmentResponse createAssignment(String ownerId, CreateAssignmentRequest request) {
        validateAssignment(ownerId, request);

        Asset asset = assetRepository.findByAssetIdAndOwnerId(request.getAssetId(), ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found"));

        Contact executor = contactRepository.findByContactIdAndOwnerId(request.getExecutorContactId(), ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Executor not found"));

        List<String> distinctBeneficiaryIds = extractDistinctBeneficiaryIds(request.getBeneficiaryContactIds());

        // Save Assignment (instructions contain text notes & custom split descriptions)
        AssetAssignment assignment = new AssetAssignment(
                request.getAssetId(),
                request.getExecutorContactId(),
                request.getHandoverInstructions()
        );
        String protocol = (request.getHandoverProtocol() != null && !request.getHandoverProtocol().isBlank())
                ? request.getHandoverProtocol().trim()
                : "legal_verification";
        assignment.setHandoverProtocol(protocol);

        AssetAssignment savedAssignment = assignmentRepository.save(assignment);

        // Save Beneficiaries (linking table)
        List<BeneficiaryDto> savedBeneficiaries = new ArrayList<>();
        for (String beneficiaryId : distinctBeneficiaryIds) {
            AssignmentBeneficiary link = new AssignmentBeneficiary(
                    savedAssignment.getAssignmentId(),
                    beneficiaryId
            );
            beneficiaryRepository.save(link);

            Contact c = contactRepository.findById(beneficiaryId).orElse(null);
            savedBeneficiaries.add(new BeneficiaryDto(
                    beneficiaryId,
                    c != null ? c.getFullName() : "",
                    c != null ? c.getEmail() : "",
                    c != null ? c.getRelationship() : ""
            ));
        }

        // Update asset status to 'assigned'
        asset.setStatus("assigned");
        assetRepository.save(asset);

        return new AssignmentResponse(
                savedAssignment.getAssignmentId(),
                asset.getAssetId(),
                asset.getAssetName(),
                asset.getAssetType(),
                executor.getContactId(),
                executor.getFullName(),
                executor.getEmail(),
                savedAssignment.getHandoverProtocol(),
                savedAssignment.getHandoverInstructions(),
                savedBeneficiaries,
                savedAssignment.getCreatedAt(),
                savedAssignment.getUpdatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> getAssignmentsByOwner(String ownerId) {
        List<Asset> assets = assetRepository.findByOwnerId(ownerId);
        List<AssignmentResponse> responses = new ArrayList<>();

        for (Asset asset : assets) {
            assignmentRepository.findByAssetId(asset.getAssetId()).ifPresent(assignment -> {
                Contact executor = contactRepository.findById(assignment.getExecutorContactId()).orElse(null);
                List<AssignmentBeneficiary> splits = beneficiaryRepository.findByAssignmentId(assignment.getAssignmentId());

                List<BeneficiaryDto> beneficiaryDtos = splits.stream().map(s -> {
                    Contact c = contactRepository.findById(s.getBeneficiaryContactId()).orElse(null);
                    return new BeneficiaryDto(
                            s.getBeneficiaryContactId(),
                            c != null ? c.getFullName() : "",
                            c != null ? c.getEmail() : "",
                            c != null ? c.getRelationship() : ""
                    );
                }).collect(Collectors.toList());

                responses.add(new AssignmentResponse(
                        assignment.getAssignmentId(),
                        asset.getAssetId(),
                        asset.getAssetName(),
                        asset.getAssetType(),
                        executor != null ? executor.getContactId() : assignment.getExecutorContactId(),
                        executor != null ? executor.getFullName() : "",
                        executor != null ? executor.getEmail() : "",
                        assignment.getHandoverProtocol(),
                        assignment.getHandoverInstructions(),
                        beneficiaryDtos,
                        assignment.getCreatedAt(),
                        assignment.getUpdatedAt()
                ));
            });
        }

        return responses;
    }

    @Transactional(readOnly = true)
    public AssignmentResponse getAssignmentByAssetId(String ownerId, String assetId) {
        Asset asset = assetRepository.findByAssetIdAndOwnerId(assetId, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found or access denied"));

        AssetAssignment assignment = assignmentRepository.findByAssetId(assetId)
                .orElseThrow(() -> new RuntimeException("Assignment not found for this asset"));

        Contact executor = contactRepository.findById(assignment.getExecutorContactId()).orElse(null);
        List<AssignmentBeneficiary> splits = beneficiaryRepository.findByAssignmentId(assignment.getAssignmentId());

        List<BeneficiaryDto> beneficiaryDtos = splits.stream().map(s -> {
            Contact c = contactRepository.findById(s.getBeneficiaryContactId()).orElse(null);
            return new BeneficiaryDto(
                    s.getBeneficiaryContactId(),
                    c != null ? c.getFullName() : "",
                    c != null ? c.getEmail() : "",
                    c != null ? c.getRelationship() : ""
            );
        }).collect(Collectors.toList());

        return new AssignmentResponse(
                assignment.getAssignmentId(),
                asset.getAssetId(),
                asset.getAssetName(),
                asset.getAssetType(),
                executor != null ? executor.getContactId() : assignment.getExecutorContactId(),
                executor != null ? executor.getFullName() : "",
                executor != null ? executor.getEmail() : "",
                assignment.getHandoverProtocol(),
                assignment.getHandoverInstructions(),
                beneficiaryDtos,
                assignment.getCreatedAt(),
                assignment.getUpdatedAt()
        );
    }

    @Transactional
    public AssignmentResponse updateAssignmentByAssetId(String ownerId, String assetId, UpdateAssignmentRequest request) {
        if (ownerId == null || ownerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Owner ID is required");
        }
        if (assetId == null || assetId.trim().isEmpty()) {
            throw new IllegalArgumentException("Asset ID is required");
        }
        if (request == null) {
            throw new IllegalArgumentException("Request body cannot be null");
        }

        List<String> distinctBeneficiaryIds = extractDistinctBeneficiaryIds(request.getBeneficiaryContactIds());
        if (distinctBeneficiaryIds.isEmpty()) {
            throw new IllegalArgumentException("At least one beneficiary contact ID is required");
        }

        // 1. Validate Asset ownership
        Asset asset = assetRepository.findByAssetIdAndOwnerId(assetId, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found or access denied"));

        // 2. Find existing assignment by assetId
        AssetAssignment assignment = assignmentRepository.findByAssetId(assetId)
                .orElseThrow(() -> new RuntimeException("Assignment not found for this asset"));

        // 3. Validate Executor contact
        Contact executor = contactRepository.findByContactIdAndOwnerId(request.getExecutorContactId(), ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Executor contact not found or access denied"));
        ensureContactIsVerified(executor, "Người thi hành");
        if (!Boolean.TRUE.equals(executor.getIsExecutor())) {
            throw new IllegalArgumentException("Liên hệ \"" + executor.getFullName() + "\" không có vai trò Người thi hành (Executor).");
        }

        // 4. Validate Beneficiary contacts
        for (String beneficiaryId : distinctBeneficiaryIds) {
            Contact beneficiary = contactRepository.findByContactIdAndOwnerId(beneficiaryId, ownerId)
                    .orElseThrow(() -> new IllegalArgumentException("Beneficiary contact not found: " + beneficiaryId));
            ensureContactIsVerified(beneficiary, "Người thụ hưởng");
            if (!Boolean.TRUE.equals(beneficiary.getIsBeneficiary())) {
                throw new IllegalArgumentException("Liên hệ \"" + beneficiary.getFullName() + "\" không có vai trò Người thụ hưởng (Beneficiary).");
            }
        }

        // 5. Update Assignment details
        assignment.setExecutorContactId(request.getExecutorContactId());
        if (request.getHandoverProtocol() != null && !request.getHandoverProtocol().isBlank()) {
            assignment.setHandoverProtocol(request.getHandoverProtocol().trim());
        }
        assignment.setHandoverInstructions(request.getHandoverInstructions());
        assignment.setUpdatedAt(LocalDateTime.now());
        AssetAssignment savedAssignment = assignmentRepository.save(assignment);

        // 6. Update Beneficiaries (remove old ones and add new ones)
        beneficiaryRepository.deleteByAssignmentId(savedAssignment.getAssignmentId());
        beneficiaryRepository.flush();

        List<BeneficiaryDto> savedBeneficiaries = new ArrayList<>();
        for (String beneficiaryId : distinctBeneficiaryIds) {
            AssignmentBeneficiary link = new AssignmentBeneficiary(
                    savedAssignment.getAssignmentId(),
                    beneficiaryId
            );
            beneficiaryRepository.save(link);

            Contact c = contactRepository.findById(beneficiaryId).orElse(null);
            savedBeneficiaries.add(new BeneficiaryDto(
                    beneficiaryId,
                    c != null ? c.getFullName() : "",
                    c != null ? c.getEmail() : "",
                    c != null ? c.getRelationship() : ""
            ));
        }

        // 7. Ensure asset status is 'assigned'
        asset.setStatus("assigned");
        assetRepository.save(asset);

        return new AssignmentResponse(
                savedAssignment.getAssignmentId(),
                asset.getAssetId(),
                asset.getAssetName(),
                asset.getAssetType(),
                executor.getContactId(),
                executor.getFullName(),
                executor.getEmail(),
                savedAssignment.getHandoverProtocol(),
                savedAssignment.getHandoverInstructions(),
                savedBeneficiaries,
                savedAssignment.getCreatedAt(),
                savedAssignment.getUpdatedAt()
        );
    }

    @Transactional
    public void deleteAssignmentByAssetId(String ownerId, String assetId) {
        if (ownerId == null || ownerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Owner ID is required");
        }
        if (assetId == null || assetId.trim().isEmpty()) {
            throw new IllegalArgumentException("Asset ID is required");
        }

        Asset asset = assetRepository.findByAssetIdAndOwnerId(assetId.trim(), ownerId.trim())
                .orElseThrow(() -> new IllegalArgumentException("Asset not found or access denied"));

        AssetAssignment assignment = assignmentRepository.findByAssetId(asset.getAssetId())
                .orElseThrow(() -> new RuntimeException("Assignment not found for this asset"));

        beneficiaryRepository.deleteByAssignmentId(assignment.getAssignmentId());
        assignmentRepository.delete(assignment);

        asset.setStatus("unassigned");
        assetRepository.save(asset);
    }

    private List<String> extractDistinctBeneficiaryIds(List<String> rawIds) {
        if (rawIds == null) {
            return List.of();
        }
        return rawIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
    }

    private void ensureContactIsVerified(Contact contact, String roleLabel) {
        String status = contact.getStatus() != null ? contact.getStatus().trim().toLowerCase() : "pending";
        if (!"verified".equals(status)) {
            String name = contact.getFullName() != null ? contact.getFullName() : "Contact";
            if ("rejected".equals(status) || "declined".equals(status) || "revoked".equals(status)) {
                throw new IllegalArgumentException(
                        roleLabel + " \"" + name + "\" đã từ chối hoặc bị thu hồi lời mời. Vui lòng chờ người liên hệ đồng ý thì bạn mới được gán."
                );
            }
            throw new IllegalArgumentException(
                    roleLabel + " \"" + name + "\" đang ở trạng thái chờ xác nhận (Pending). Vui lòng chờ người liên hệ đồng ý thì bạn mới được gán."
            );
        }
    }
}
