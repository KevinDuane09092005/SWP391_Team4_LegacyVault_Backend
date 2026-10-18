package com.legacyvault.service;

import com.legacyvault.dto.AuthorizationEvaluationResponse;
import com.legacyvault.dto.ExecutorHandoverDashboardResponse;
import com.legacyvault.dto.HandoverDetailsResponse;
import com.legacyvault.dto.StartReleaseResponse;
import com.legacyvault.entity.*;
import com.legacyvault.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HandoverExecutionServiceTest {

    @Mock
    private HandoverCaseRepository handoverCaseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ContactRepository contactRepository;

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private AssetAssignmentRepository assetAssignmentRepository;

    @Mock
    private AssignmentBeneficiaryRepository assignmentBeneficiaryRepository;

    @Mock
    private BeneficiaryClaimSessionRepository claimSessionRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private HandoverExecutionService handoverExecutionService;

    private HandoverCase mockCase;
    private User mockOwner;
    private Contact mockBeneficiary;

    @BeforeEach
    void setUp() {
        mockCase = new HandoverCase("LV-1042", "owner-uuid-1", "dms-uuid-1");
        mockCase.setCaseId("case-uuid-1042");
        mockCase.setStatus("APPROVED");
        mockCase.setLegalVerificationSummary("Certified Court Order Verified");

        mockOwner = new User();
        mockOwner.setId("owner-uuid-1");
        mockOwner.setFullName("Thanh Pham");
        mockOwner.setEmail("thanh.pham@vault-owner.com");
        mockOwner.setRole("VAULT_OWNER");

        mockBeneficiary = new Contact("owner-uuid-1", "Linh Pham", "Daughter", "linh.pham@beneficiary.com", "+1 (555) 234 882", true, false, "active");
        mockBeneficiary.setContactId("beneficiary-uuid-1");
    }

    @Test
    void testGetExecutorDashboard() {
        when(handoverCaseRepository.findAll()).thenReturn(List.of(mockCase));
        when(userRepository.findById("owner-uuid-1")).thenReturn(Optional.of(mockOwner));
        when(contactRepository.findByOwnerId("owner-uuid-1")).thenReturn(List.of(mockBeneficiary));

        ExecutorHandoverDashboardResponse dashboard = handoverExecutionService.getExecutorDashboard("executor-1");

        assertNotNull(dashboard);
        assertNotNull(dashboard.getMetrics());
        assertEquals(1, dashboard.getCases().size());
        assertEquals("LV-1042", dashboard.getCases().get(0).getCaseNumber());
        assertEquals("Thanh Pham", dashboard.getCases().get(0).getVaultOwner());
        assertEquals("Linh Pham", dashboard.getCases().get(0).getBeneficiary());
    }

    @Test
    void testGetHandoverDetails() {
        when(handoverCaseRepository.findByCaseNumber("LV-1042")).thenReturn(Optional.of(mockCase));
        when(userRepository.findById("owner-uuid-1")).thenReturn(Optional.of(mockOwner));
        when(contactRepository.findByOwnerId("owner-uuid-1")).thenReturn(List.of(mockBeneficiary));
        when(assetRepository.findByOwnerId("owner-uuid-1")).thenReturn(Collections.emptyList());

        HandoverDetailsResponse details = handoverExecutionService.getHandoverDetails("LV-1042");

        assertNotNull(details);
        assertEquals("LV-1042", details.getCaseNumber());
        assertEquals("Thanh Pham", details.getVaultOwner().getName());
        assertEquals("Linh Pham", details.getPrimaryBeneficiary().getName());
        assertEquals("APPROVED", details.getLegalVerification().getStatus());
        assertFalse(details.getAuthorizedAssets().isEmpty());
    }

    @Test
    void testStartHandoverRelease_Success() {
        when(handoverCaseRepository.findByCaseNumber("LV-1042")).thenReturn(Optional.of(mockCase));
        when(contactRepository.findByOwnerId("owner-uuid-1")).thenReturn(List.of(mockBeneficiary));
        when(userRepository.findById("owner-uuid-1")).thenReturn(Optional.of(mockOwner));
        when(claimSessionRepository.findByCaseIdAndBeneficiaryContactId(anyString(), anyString())).thenReturn(Optional.empty());

        StartReleaseResponse response = handoverExecutionService.startHandoverRelease("LV-1042", "Minh Le (Executor)");

        assertNotNull(response);
        assertEquals("LV-1042", response.getCaseNumber());
        assertEquals("LV-1042-HANDOVER-SECURE", response.getAccessCode());
        assertEquals("linh.pham@beneficiary.com", response.getNotificationSentTo());
        assertEquals("IN PROGRESS", mockCase.getStatus());
        verify(emailService).sendHandoverAccessCodeEmail(eq("linh.pham@beneficiary.com"), eq("Linh Pham"), eq("Thanh Pham"), eq("LV-1042"), eq("LV-1042-HANDOVER-SECURE"));
    }

    @Test
    void testStartHandoverRelease_ThrowsIfNotApproved() {
        mockCase.setStatus("PENDING");
        when(handoverCaseRepository.findByCaseNumber("LV-1042")).thenReturn(Optional.of(mockCase));

        assertThrows(IllegalStateException.class, () ->
                handoverExecutionService.startHandoverRelease("LV-1042", "executor-1"));
    }

    @Test
    void testGetAuthorizationEvaluation() {
        when(handoverCaseRepository.findByCaseNumber("LV-1042")).thenReturn(Optional.of(mockCase));
        when(userRepository.findById("owner-uuid-1")).thenReturn(Optional.of(mockOwner));
        when(contactRepository.findByOwnerId("owner-uuid-1")).thenReturn(List.of(mockBeneficiary));

        AuthorizationEvaluationResponse response = handoverExecutionService.getAuthorizationEvaluation("LV-1042");

        assertNotNull(response);
        assertEquals("VERIFIED", response.getChecklist().getBeneficiaryIdentityVerified());
        assertEquals("APPROVED", response.getChecklist().getCertifiedCourtReleaseOrdersVerified());
        assertEquals(3, response.getAuthorizationResults().size());
        assertEquals("AUTHORIZED FOR RELEASE", response.getAuthorizationResults().get(0).getVerdict());
    }

    @Test
    void testDispatchSecureHandover() {
        when(handoverCaseRepository.findByCaseNumber("LV-1042")).thenReturn(Optional.of(mockCase));

        Map<String, String> result = handoverExecutionService.dispatchSecureHandover("LV-1042", "Minh Le");

        assertNotNull(result);
        assertEquals("RELEASED", result.get("status"));
        assertEquals("RELEASED", mockCase.getStatus());
        verify(handoverCaseRepository).save(mockCase);
    }
}
