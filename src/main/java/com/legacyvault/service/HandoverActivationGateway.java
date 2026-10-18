package com.legacyvault.service;

import com.legacyvault.entity.Asset;
import com.legacyvault.entity.AssetAssignment;
import com.legacyvault.entity.Contact;
import com.legacyvault.entity.DeadMansSwitch;
import com.legacyvault.entity.HandoverCase;
import com.legacyvault.entity.User;
import com.legacyvault.repository.AssetAssignmentRepository;
import com.legacyvault.repository.AssetRepository;
import com.legacyvault.repository.AssignmentBeneficiaryRepository;
import com.legacyvault.repository.ContactRepository;
import com.legacyvault.repository.DeadMansSwitchRepository;
import com.legacyvault.repository.HandoverCaseRepository;
import com.legacyvault.repository.UserRepository;
import org.springframework.stereotype.Component;

@Component
public class HandoverActivationGateway implements ActivationGateway {

    private final HandoverCaseRepository handoverCaseRepository;
    private final DeadMansSwitchRepository deadMansSwitchRepository;
    private final AssetAssignmentRepository assetAssignmentRepository;
    private final AssetRepository assetRepository;
    private final AssignmentBeneficiaryRepository assignmentBeneficiaryRepository;
    private final ContactRepository contactRepository;
    private final UserRepository userRepository;

    public HandoverActivationGateway(HandoverCaseRepository handoverCaseRepository,
                                     DeadMansSwitchRepository deadMansSwitchRepository,
                                     AssetAssignmentRepository assetAssignmentRepository,
                                     AssetRepository assetRepository,
                                     AssignmentBeneficiaryRepository assignmentBeneficiaryRepository,
                                     ContactRepository contactRepository,
                                     UserRepository userRepository) {
        this.handoverCaseRepository = handoverCaseRepository;
        this.deadMansSwitchRepository = deadMansSwitchRepository;
        this.assetAssignmentRepository = assetAssignmentRepository;
        this.assetRepository = assetRepository;
        this.assignmentBeneficiaryRepository = assignmentBeneficiaryRepository;
        this.contactRepository = contactRepository;
        this.userRepository = userRepository;
    }

    @Override
    public VerificationTarget requireExecutorAssignment(String handoverCaseId, String assignmentId,
                                                         String beneficiaryContactId, String executorUserId) {
        HandoverCase handoverCase = handoverCaseRepository.findById(handoverCaseId)
                .orElseThrow(Flow5Exception::notFound);
        DeadMansSwitch dms = deadMansSwitchRepository.findById(handoverCase.getDmsId())
                .orElseThrow(Flow5Exception::notFound);
        if (!"triggered".equalsIgnoreCase(dms.getStatus())
                || !handoverCase.getOwnerId().equals(dms.getOwnerId())) {
            throw Flow5Exception.conflict("The handover must be triggered before legal verification can start.");
        }

        User executorUser = userRepository.findById(executorUserId)
                .orElseThrow(Flow5Exception::forbidden);
        AssetAssignment assignment = assetAssignmentRepository.findById(assignmentId)
                .orElseThrow(Flow5Exception::notFound);
        Asset asset = assetRepository.findById(assignment.getAssetId())
                .orElseThrow(Flow5Exception::notFound);
        if (!handoverCase.getOwnerId().equals(asset.getOwnerId())) {
            throw Flow5Exception.forbidden();
        }

        Contact executorContact = contactRepository.findById(assignment.getExecutorContactId())
                .orElseThrow(Flow5Exception::forbidden);
        if (!handoverCase.getOwnerId().equals(executorContact.getOwnerId())
                || !Boolean.TRUE.equals(executorContact.getIsExecutor())
                || executorUser.getEmail() == null
                || !executorUser.getEmail().equalsIgnoreCase(executorContact.getEmail())) {
            throw Flow5Exception.forbidden();
        }

        Contact beneficiary = contactRepository.findById(beneficiaryContactId)
                .orElseThrow(Flow5Exception::notFound);
        if (!handoverCase.getOwnerId().equals(beneficiary.getOwnerId())
                || !Boolean.TRUE.equals(beneficiary.getIsBeneficiary())
                || !assignmentBeneficiaryRepository.existsByAssignmentIdAndBeneficiaryContactId(
                        assignmentId, beneficiaryContactId)) {
            throw Flow5Exception.forbidden();
        }

        return new VerificationTarget(handoverCase, assignment, beneficiary);
    }
}
