package com.legacyvault.service;

import com.legacyvault.dto.AssignmentResponse;
import com.legacyvault.dto.CreateAssignmentRequest;
import com.legacyvault.dto.UpdateAssignmentRequest;
import com.legacyvault.entity.Asset;
import com.legacyvault.entity.AssetAssignment;
import com.legacyvault.entity.AssignmentBeneficiary;
import com.legacyvault.entity.Contact;
import com.legacyvault.repository.AssetAssignmentRepository;
import com.legacyvault.repository.AssetRepository;
import com.legacyvault.repository.AssignmentBeneficiaryRepository;
import com.legacyvault.repository.ContactRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AssignmentServiceTest {

    @Mock
    private AssetAssignmentRepository assignmentRepository;

    @Mock
    private AssignmentBeneficiaryRepository beneficiaryRepository;

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private ContactRepository contactRepository;

    @InjectMocks
    private AssignmentService assignmentService;

    private Asset testAsset;
    private Contact testExecutor;
    private Contact testBeneficiary1;
    private Contact testBeneficiary2;
    private CreateAssignmentRequest validRequest;

    @BeforeEach
    void setUp() {
        testAsset = new Asset("owner-1", "Crypto Wallet", "MetaMask Wallet (ETH)", "Primary wallet", "encrypted", null);
        testAsset.setAssetId("asset-1");

        testExecutor = new Contact("owner-1", "Le Van D", "Attorney", "levand@email.com", "0123", false, true, "verified");
        testExecutor.setContactId("executor-1");

        testBeneficiary1 = new Contact("owner-1", "Tran Thi B", "Spouse", "tranthib@email.com", "0123", true, false, "verified");
        testBeneficiary1.setContactId("beneficiary-1");

        testBeneficiary2 = new Contact("owner-1", "Nguyen Van C", "Friend", "nguyenc@email.com", "0123", true, false, "verified");
        testBeneficiary2.setContactId("beneficiary-2");

        List<String> beneficiaryIds = List.of("beneficiary-1", "beneficiary-2");
        String handoverText = "Transfer upon DMS trigger. Split: Tran Thi B (Spouse): 60%, Nguyen Van C (Friend): 40%";

        validRequest = new CreateAssignmentRequest("asset-1", "executor-1", handoverText, beneficiaryIds);
    }

    @Test
    void testValidateAssignment_Success() {
        when(assetRepository.findByAssetIdAndOwnerId("asset-1", "owner-1")).thenReturn(Optional.of(testAsset));
        when(assignmentRepository.existsByAssetId("asset-1")).thenReturn(false);
        when(contactRepository.findByContactIdAndOwnerId("executor-1", "owner-1")).thenReturn(Optional.of(testExecutor));
        when(contactRepository.findByContactIdAndOwnerId("beneficiary-1", "owner-1")).thenReturn(Optional.of(testBeneficiary1));
        when(contactRepository.findByContactIdAndOwnerId("beneficiary-2", "owner-1")).thenReturn(Optional.of(testBeneficiary2));

        Map<String, Object> result = assignmentService.validateAssignment("owner-1", validRequest);

        assertNotNull(result);
        assertEquals(true, result.get("valid"));
        assertEquals("MetaMask Wallet (ETH)", result.get("assetName"));
        assertEquals("Le Van D", result.get("executorName"));
        assertEquals(2, result.get("beneficiariesCount"));
    }

    @Test
    void testValidateAssignment_UnverifiedContact_ThrowsException() {
        Contact pendingBeneficiary = new Contact("owner-1", "OGGY", "Child", "oggy@email.com", "0123", true, false, "pending");
        pendingBeneficiary.setContactId("beneficiary-pending");

        CreateAssignmentRequest pendingRequest = new CreateAssignmentRequest(
                "asset-1", "executor-1", "Instructions", List.of("beneficiary-pending")
        );

        when(assetRepository.findByAssetIdAndOwnerId("asset-1", "owner-1")).thenReturn(Optional.of(testAsset));
        when(assignmentRepository.existsByAssetId("asset-1")).thenReturn(false);
        when(contactRepository.findByContactIdAndOwnerId("executor-1", "owner-1")).thenReturn(Optional.of(testExecutor));
        when(contactRepository.findByContactIdAndOwnerId("beneficiary-pending", "owner-1")).thenReturn(Optional.of(pendingBeneficiary));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                assignmentService.validateAssignment("owner-1", pendingRequest)
        );
        assertTrue(ex.getMessage().contains("chờ người liên hệ đồng ý"));
    }

    @Test
    void testValidateAssignment_NoBeneficiaries_ThrowsException() {
        CreateAssignmentRequest emptyBeneficiariesRequest = new CreateAssignmentRequest(
                "asset-1", "executor-1", "Instructions", Collections.emptyList()
        );

        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                assignmentService.validateAssignment("owner-1", emptyBeneficiariesRequest)
        );

        assertTrue(exception.getMessage().contains("At least one beneficiary contact ID is required"));
    }

    @Test
    void testCreateAssignment_Success() {
        when(assetRepository.findByAssetIdAndOwnerId("asset-1", "owner-1")).thenReturn(Optional.of(testAsset));
        when(assignmentRepository.existsByAssetId("asset-1")).thenReturn(false);
        when(contactRepository.findByContactIdAndOwnerId("executor-1", "owner-1")).thenReturn(Optional.of(testExecutor));
        when(contactRepository.findByContactIdAndOwnerId("beneficiary-1", "owner-1")).thenReturn(Optional.of(testBeneficiary1));
        when(contactRepository.findByContactIdAndOwnerId("beneficiary-2", "owner-1")).thenReturn(Optional.of(testBeneficiary2));

        when(assignmentRepository.save(any(AssetAssignment.class))).thenAnswer(invocation -> {
            AssetAssignment a = invocation.getArgument(0);
            a.setAssignmentId("assignment-uuid-1");
            return a;
        });

        when(contactRepository.findById("beneficiary-1")).thenReturn(Optional.of(testBeneficiary1));
        when(contactRepository.findById("beneficiary-2")).thenReturn(Optional.of(testBeneficiary2));

        AssignmentResponse response = assignmentService.createAssignment("owner-1", validRequest);

        assertNotNull(response);
        assertEquals("asset-1", response.getAssetId());
        assertEquals("executor-1", response.getExecutorContactId());
        assertEquals(2, response.getBeneficiaries().size());
        assertTrue(response.getHandoverInstructions().contains("Tran Thi B (Spouse): 60%"));
        assertEquals("assigned", testAsset.getStatus());
        verify(assetRepository, times(1)).save(testAsset);
        verify(beneficiaryRepository, times(2)).save(any(AssignmentBeneficiary.class));
    }

    @Test
    void testCreateAssignment_AssetAlreadyAssigned_ThrowsException() {
        when(assetRepository.findByAssetIdAndOwnerId("asset-1", "owner-1")).thenReturn(Optional.of(testAsset));
        when(assignmentRepository.existsByAssetId("asset-1")).thenReturn(true);

        Exception exception = assertThrows(IllegalStateException.class, () ->
                assignmentService.createAssignment("owner-1", validRequest)
        );

        assertTrue(exception.getMessage().contains("Asset already has an active assignment"));
    }

    @Test
    void testUpdateAssignmentByAssetId_Success() {
        AssetAssignment existingAssignment = new AssetAssignment("asset-1", "executor-old", "Old instructions");
        existingAssignment.setAssignmentId("assignment-1");

        UpdateAssignmentRequest updateRequest = new UpdateAssignmentRequest(
                "asset-1",
                "executor-1",
                "Updated instructions: 100% to Tran Thi B",
                List.of("beneficiary-1")
        );

        when(assetRepository.findByAssetIdAndOwnerId("asset-1", "owner-1")).thenReturn(Optional.of(testAsset));
        when(assignmentRepository.findByAssetId("asset-1")).thenReturn(Optional.of(existingAssignment));
        when(contactRepository.findByContactIdAndOwnerId("executor-1", "owner-1")).thenReturn(Optional.of(testExecutor));
        when(contactRepository.findByContactIdAndOwnerId("beneficiary-1", "owner-1")).thenReturn(Optional.of(testBeneficiary1));
        when(assignmentRepository.save(any(AssetAssignment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(contactRepository.findById("beneficiary-1")).thenReturn(Optional.of(testBeneficiary1));

        AssignmentResponse response = assignmentService.updateAssignmentByAssetId("owner-1", "asset-1", updateRequest);

        assertNotNull(response);
        assertEquals("assignment-1", response.getAssignmentId());
        assertEquals("asset-1", response.getAssetId());
        assertEquals("executor-1", response.getExecutorContactId());
        assertEquals("Updated instructions: 100% to Tran Thi B", response.getHandoverInstructions());
        assertEquals(1, response.getBeneficiaries().size());
        assertEquals("beneficiary-1", response.getBeneficiaries().get(0).getContactId());

        verify(beneficiaryRepository, times(1)).deleteByAssignmentId("assignment-1");
        verify(beneficiaryRepository, times(1)).flush();
        verify(beneficiaryRepository, times(1)).save(any(AssignmentBeneficiary.class));
        verify(assignmentRepository, times(1)).save(existingAssignment);
        verify(assetRepository, times(1)).save(testAsset);
    }

    @Test
    void testUpdateAssignmentByAssetId_NotFound_ThrowsException() {
        UpdateAssignmentRequest updateRequest = new UpdateAssignmentRequest(
                "executor-1",
                "Updated instructions",
                List.of("beneficiary-1")
        );

        when(assetRepository.findByAssetIdAndOwnerId("non-existent", "owner-1")).thenReturn(Optional.empty());

        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                assignmentService.updateAssignmentByAssetId("owner-1", "non-existent", updateRequest)
        );

        assertTrue(exception.getMessage().contains("Asset not found"));
    }

    @Test
    void testUpdateAssignmentByAssetId_AssignmentNotFound_ThrowsException() {
        UpdateAssignmentRequest updateRequest = new UpdateAssignmentRequest(
                "executor-1",
                "Updated instructions",
                List.of("beneficiary-1")
        );

        when(assetRepository.findByAssetIdAndOwnerId("asset-1", "owner-1")).thenReturn(Optional.of(testAsset));
        when(assignmentRepository.findByAssetId("asset-1")).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () ->
                assignmentService.updateAssignmentByAssetId("owner-1", "asset-1", updateRequest)
        );

        assertTrue(exception.getMessage().contains("Assignment not found for this asset"));
    }

    @Test
    void testUpdateAssignmentByAssetId_InvalidExecutor_ThrowsException() {
        AssetAssignment existingAssignment = new AssetAssignment("asset-1", "executor-old", "Old instructions");
        existingAssignment.setAssignmentId("assignment-1");

        UpdateAssignmentRequest updateRequest = new UpdateAssignmentRequest(
                "asset-1",
                "executor-invalid",
                "Updated instructions",
                List.of("beneficiary-1")
        );

        when(assetRepository.findByAssetIdAndOwnerId("asset-1", "owner-1")).thenReturn(Optional.of(testAsset));
        when(assignmentRepository.findByAssetId("asset-1")).thenReturn(Optional.of(existingAssignment));
        when(contactRepository.findByContactIdAndOwnerId("executor-invalid", "owner-1")).thenReturn(Optional.empty());

        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                assignmentService.updateAssignmentByAssetId("owner-1", "asset-1", updateRequest)
        );

        assertTrue(exception.getMessage().contains("Executor contact not found"));
    }
}
