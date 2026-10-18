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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HandoverActivationGatewayTest {

    private final HandoverCaseRepository handoverCaseRepository = mock(HandoverCaseRepository.class);
    private final DeadMansSwitchRepository deadMansSwitchRepository = mock(DeadMansSwitchRepository.class);
    private final AssetAssignmentRepository assetAssignmentRepository = mock(AssetAssignmentRepository.class);
    private final AssetRepository assetRepository = mock(AssetRepository.class);
    private final AssignmentBeneficiaryRepository assignmentBeneficiaryRepository = mock(AssignmentBeneficiaryRepository.class);
    private final ContactRepository contactRepository = mock(ContactRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);

    private final HandoverActivationGateway gateway = new HandoverActivationGateway(
            handoverCaseRepository, deadMansSwitchRepository, assetAssignmentRepository,
            assetRepository, assignmentBeneficiaryRepository, contactRepository, userRepository);

    private HandoverCase handoverCase;
    private DeadMansSwitch dms;
    private AssetAssignment assignment;
    private Asset asset;
    private Contact executorContact;
    private Contact beneficiary;
    private User executor;
    private String handoverCaseId;
    private String assignmentId;
    private String beneficiaryId;
    private String executorId;

    @BeforeEach
    void setUp() {
        String ownerId = UUID.randomUUID().toString();
        handoverCaseId = UUID.randomUUID().toString();
        String dmsId = UUID.randomUUID().toString();
        assignmentId = UUID.randomUUID().toString();
        beneficiaryId = UUID.randomUUID().toString();
        executorId = UUID.randomUUID().toString();
        String executorContactId = UUID.randomUUID().toString();
        String assetId = UUID.randomUUID().toString();

        handoverCase = new HandoverCase("HC-TEST", ownerId, dmsId);
        handoverCase.setCaseId(handoverCaseId);
        dms = new DeadMansSwitch(ownerId, 30, 7);
        dms.setDmsId(dmsId);
        dms.setStatus("triggered");

        assignment = new AssetAssignment(assetId, executorContactId, null);
        assignment.setAssignmentId(assignmentId);
        asset = new Asset();
        asset.setAssetId(assetId);
        asset.setOwnerId(ownerId);

        executorContact = new Contact(ownerId, "Executor", "Friend", "executor@example.com",
                null, false, true, "verified");
        executorContact.setContactId(executorContactId);
        beneficiary = new Contact(ownerId, "Beneficiary", "Child", "beneficiary@example.com",
                null, true, false, "verified");
        beneficiary.setContactId(beneficiaryId);
        executor = new User();
        executor.setId(executorId);
        executor.setEmail("executor@example.com");

        when(handoverCaseRepository.findById(handoverCaseId)).thenReturn(Optional.of(handoverCase));
        when(deadMansSwitchRepository.findById(dmsId)).thenReturn(Optional.of(dms));
        when(userRepository.findById(executorId)).thenReturn(Optional.of(executor));
        when(assetAssignmentRepository.findById(assignmentId)).thenReturn(Optional.of(assignment));
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        when(contactRepository.findById(executorContactId)).thenReturn(Optional.of(executorContact));
        when(contactRepository.findById(beneficiaryId)).thenReturn(Optional.of(beneficiary));
        when(assignmentBeneficiaryRepository.existsByAssignmentIdAndBeneficiaryContactId(
                assignmentId, beneficiaryId)).thenReturn(true);
    }

    @Test
    void acceptsTriggeredHandoverAndMatchingAssignedContacts() {
        VerificationTarget target = gateway.requireExecutorAssignment(
                handoverCaseId, assignmentId, beneficiaryId, executorId);

        assertEquals(handoverCase, target.handoverCase());
        assertEquals(assignment, target.assignment());
        assertEquals(beneficiary, target.beneficiaryContact());
    }

    @Test
    void rejectsWhenDmsHasNotTriggered() {
        dms.setStatus("grace_period");

        Flow5Exception error = assertThrows(Flow5Exception.class, () -> gateway.requireExecutorAssignment(
                handoverCaseId, assignmentId, beneficiaryId, executorId));

        assertEquals(409, error.getStatus().value());
    }

    @Test
    void rejectsWhenAuthenticatedUserDoesNotMatchAssignedExecutorEmail() {
        executor.setEmail("someone-else@example.com");

        Flow5Exception error = assertThrows(Flow5Exception.class, () -> gateway.requireExecutorAssignment(
                handoverCaseId, assignmentId, beneficiaryId, executorId));

        assertEquals(403, error.getStatus().value());
    }

    @Test
    void rejectsBeneficiaryNotLinkedToTheSelectedAssignment() {
        when(assignmentBeneficiaryRepository.existsByAssignmentIdAndBeneficiaryContactId(
                assignmentId, beneficiaryId)).thenReturn(false);

        Flow5Exception error = assertThrows(Flow5Exception.class, () -> gateway.requireExecutorAssignment(
                handoverCaseId, assignmentId, beneficiaryId, executorId));

        assertEquals(403, error.getStatus().value());
    }
}
