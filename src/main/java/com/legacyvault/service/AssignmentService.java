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

        if (request.getBeneficiaryContactIds() == null || request.getBeneficiaryContactIds().isEmpty()) {
            throw new IllegalArgumentException("At least one beneficiary contact ID is required");
        }

        // Validate Asset ownership
        Asset asset = assetRepository.findByAssetIdAndOwnerId(request.getAssetId(), ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found or access denied"));

        if (assignmentRepository.existsByAssetId(request.getAssetId())) {
            throw new IllegalStateException("Asset already has an active assignment");
        }

        // Validate Executor contact
        Contact executor = contactRepository.findByContactIdAndOwnerId(request.getExecutorContactId(), ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Executor contact not found or access denied"));

        // Validate Beneficiaries contacts
        for (String beneficiaryId : request.getBeneficiaryContactIds()) {
            if (!contactRepository.existsByContactIdAndOwnerId(beneficiaryId, ownerId)) {
                throw new IllegalArgumentException("Beneficiary contact not found: " + beneficiaryId);
            }
        }

        return Map.of(
                "valid", true,
                "message", "Assignment configuration is valid",
                "assetName", asset.getAssetName(),
                "executorName", executor.getFullName(),
                "beneficiariesCount", request.getBeneficiaryContactIds().size()
        );
    }

    @Transactional
    public AssignmentResponse createAssignment(String ownerId, CreateAssignmentRequest request) {
        validateAssignment(ownerId, request);

        Asset asset = assetRepository.findByAssetIdAndOwnerId(request.getAssetId(), ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found"));

        Contact executor = contactRepository.findByContactIdAndOwnerId(request.getExecutorContactId(), ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Executor not found"));

        // Save Assignment (instructions contain text notes & custom split descriptions)
        AssetAssignment assignment = new AssetAssignment(
                request.getAssetId(),
                request.getExecutorContactId(),
                request.getHandoverInstructions()
        );
        AssetAssignment savedAssignment = assignmentRepository.save(assignment);

        // Save Beneficiaries (linking table)
        List<BeneficiaryDto> savedBeneficiaries = new ArrayList<>();
        for (String beneficiaryId : request.getBeneficiaryContactIds()) {
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
        if (request.getBeneficiaryContactIds() == null || request.getBeneficiaryContactIds().isEmpty()) {
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

        // 4. Validate Beneficiary contacts
        for (String beneficiaryId : request.getBeneficiaryContactIds()) {
            if (!contactRepository.existsByContactIdAndOwnerId(beneficiaryId, ownerId)) {
                throw new IllegalArgumentException("Beneficiary contact not found: " + beneficiaryId);
            }
        }

        // 5. Update Assignment details
        assignment.setExecutorContactId(request.getExecutorContactId());
        assignment.setHandoverInstructions(request.getHandoverInstructions());
        assignment.setUpdatedAt(LocalDateTime.now());
        AssetAssignment savedAssignment = assignmentRepository.save(assignment);

        // 6. Update Beneficiaries (remove old ones and add new ones)
        beneficiaryRepository.deleteByAssignmentId(savedAssignment.getAssignmentId());
        beneficiaryRepository.flush();

        List<BeneficiaryDto> savedBeneficiaries = new ArrayList<>();
        for (String beneficiaryId : request.getBeneficiaryContactIds()) {
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
                savedAssignment.getHandoverInstructions(),
                savedBeneficiaries,
                savedAssignment.getCreatedAt(),
                savedAssignment.getUpdatedAt()
        );
    }
}
