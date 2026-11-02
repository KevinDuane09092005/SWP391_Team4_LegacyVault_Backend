package com.legacyvault.service;

import com.legacyvault.dto.*;
import com.legacyvault.entity.*;
import com.legacyvault.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BeneficiaryHandoverServiceTest {

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
    private HandoverReceiptRepository handoverReceiptRepository;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private JwtService jwtService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private BeneficiaryHandoverService beneficiaryHandoverService;

    private HandoverCase mockCase;
    private Contact mockContact;

    @BeforeEach
    void setUp() {
        mockCase = new HandoverCase("LV-1042", "owner-uuid-1", "dms-uuid-1");
        mockCase.setCaseId("case-uuid-1042");

        mockContact = new Contact("owner-uuid-1", "Linh Pham", "Daughter", "linh.pham@beneficiary.com", "+1 (555) 234 882", true, false, "active");
        mockContact.setContactId("beneficiary-uuid-1");
    }

    @Test
    void testGetPortalContext() {
        when(handoverCaseRepository.findByCaseNumber("LV-1042")).thenReturn(Optional.of(mockCase));
        when(userRepository.findById("owner-uuid-1")).thenReturn(Optional.empty());

        BeneficiaryPortalContextResponse response = beneficiaryHandoverService.getPortalContext("LV-1042");

        assertNotNull(response);
        assertEquals("LV-1042", response.getCaseReference());
    }

    @Test
    void testGetKycStatus_IsScopedToTheRequestedCase() {
        BeneficiaryClaimSession claim = new BeneficiaryClaimSession();
        claim.setCaseId("case-uuid-1042");
        claim.setBeneficiaryContactId("beneficiary-uuid-1");
        claim.setIdentityVerified(true);
        claim.setTokenStatus("ACTIVE");
        claim.setKycPhotoIdStatus("VERIFIED");
        claim.setKycFacialStatus("FAILED");
        when(claimSessionRepository.findByCaseIdAndBeneficiaryContactId(
                "case-uuid-1042", "beneficiary-uuid-1")).thenReturn(Optional.of(claim));

        KycStatusResponse response = beneficiaryHandoverService.getKycStatus(
                "beneficiary-uuid-1", "case-uuid-1042");

        assertEquals("VERIFIED", response.getGovernmentPhotoIdStatus());
        assertEquals("FAILED", response.getBiometricFacialVerificationStatus());
    }

    @Test
    void testLoginWithCode_Success() {
        when(contactRepository.findFirstByEmail("linh.pham@beneficiary.com")).thenReturn(Optional.of(mockContact));
        when(claimSessionRepository.findByAccessCodeHash(anyString())).thenReturn(Optional.empty());

        BeneficiaryLoginSuccessResponse response = beneficiaryHandoverService.loginWithCode("linh.pham@beneficiary.com", "LV-1042-HANDOVER-SECURE");

        assertNotNull(response);
        assertEquals("Linh Pham", response.getBeneficiaryName());
        assertTrue(response.isRequiresOtp());
        verify(emailService).sendOtpEmail(eq("linh.pham@beneficiary.com"), eq("482916"));
    }

    @Test
    void testVerifyOtp_Success() {
        when(jwtService.generateToken(anyString(), anyString(), anyString())).thenReturn("mock-jwt-token");

        VerifyBeneficiaryOtpResponse response = beneficiaryHandoverService.verifyOtp("mock-session-token", "482916");

        assertNotNull(response);
        assertTrue(response.isOtpVerified());
        assertEquals("mock-jwt-token", response.getAccessToken());
    }

    @Test
    void testVerifyOtp_InvalidOtp() {
        assertThrows(IllegalArgumentException.class, () ->
                beneficiaryHandoverService.verifyOtp("mock-session-token", "999999"));
    }

    @Test
    void testGetAuthorizedAssets() {
        stubVerifiedFaceClaim();
        AuthorizedInheritanceAssetsResponse response = beneficiaryHandoverService.getAuthorizedAssets("beneficiary-uuid-1");

        assertNotNull(response);
        assertEquals("Linh Pham", response.getWelcomeUser());
        assertEquals(2, response.getReadyForHandover().size());
        assertEquals(1, response.getProbateRestricted().size());
        assertEquals("ASSET-1042-A", response.getReadyForHandover().get(0).getAssetId());
    }

    @Test
    void testGetDecryptedAssetDetail() {
        stubVerifiedFaceClaim();
        Asset mockAsset = new Asset("owner-1", "crypto_wallet", "Primary Vault Ledger Key", "desc", "encrypted-val", null);
        when(assetRepository.findById("ASSET-1042-A")).thenReturn(Optional.of(mockAsset));
        when(encryptionService.decrypt("encrypted-val")).thenReturn("autumn · harbor · classic · secure · vault · inheritance · legacy");

        DecryptedAssetDetailResponse detail = beneficiaryHandoverService.getDecryptedAssetDetail("ASSET-1042-A", "beneficiary-uuid-1");

        assertNotNull(detail);
        assertEquals("ASSET-1042-A", detail.getAssetId());
        assertEquals("VERIFIED SECURE", detail.getSecurityBadge());
        assertEquals("autumn · harbor · classic · secure · vault · inheritance · legacy", detail.getDecryptedCredentialData().getPassphrase());
    }

    @Test
    void testConfirmReceivedAndCloseCase_Success() {
        stubVerifiedFaceClaim();
        when(handoverCaseRepository.findById("case-uuid-1042")).thenReturn(Optional.of(mockCase));

        HandoverCompletedResponse response = beneficiaryHandoverService.confirmReceivedAndCloseCase(
                "beneficiary-uuid-1", true, "127.0.0.1", "Mozilla/5.0");

        assertNotNull(response);
        assertEquals("LV-1042", response.getCaseId());
        assertEquals("COMPLETED", response.getStatus());
        assertEquals("COMPLETED", mockCase.getStatus());
        verify(handoverReceiptRepository).save(any(HandoverReceipt.class));
    }

    @Test
    void testConfirmReceivedAndCloseCase_ThrowsIfNotAccepted() {
        assertThrows(IllegalArgumentException.class, () ->
                beneficiaryHandoverService.confirmReceivedAndCloseCase(
                        "beneficiary-uuid-1", false, "127.0.0.1", "Mozilla/5.0"));
    }

    @Test
    void testGetAuthorizedAssets_BlockedUntilFaceCheckPasses() {
        BeneficiaryClaimSession claim = new BeneficiaryClaimSession();
        claim.setBeneficiaryContactId("beneficiary-uuid-1");
        claim.setIdentityVerified(true);
        claim.setTokenStatus("ACTIVE");
        claim.setKycFacialStatus("PENDING_SCAN");
        when(claimSessionRepository.findByBeneficiaryContactIdAndTokenStatus(
                "beneficiary-uuid-1", "ACTIVE")).thenReturn(java.util.List.of(claim));

        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> beneficiaryHandoverService.getAuthorizedAssets("beneficiary-uuid-1"));
    }

    @Test
    void testGetAuthorizedAssets_UsesTheRequestedCaseFaceStatus() {
        BeneficiaryClaimSession otherCase = new BeneficiaryClaimSession();
        otherCase.setCaseId("another-case-id");
        otherCase.setBeneficiaryContactId("beneficiary-uuid-1");
        otherCase.setIdentityVerified(true);
        otherCase.setTokenStatus("ACTIVE");
        otherCase.setKycFacialStatus("FAILED");
        when(claimSessionRepository.findByCaseIdAndBeneficiaryContactId(
                "another-case-id", "beneficiary-uuid-1")).thenReturn(Optional.of(otherCase));

        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> beneficiaryHandoverService.getAuthorizedAssets(
                        "another-case-id", "beneficiary-uuid-1"));
    }

    private void stubVerifiedFaceClaim() {
        BeneficiaryClaimSession claim = new BeneficiaryClaimSession();
        claim.setBeneficiaryContactId("beneficiary-uuid-1");
        claim.setCaseId("case-uuid-1042");
        claim.setTokenStatus("ACTIVE");
        claim.setIdentityVerified(true);
        claim.setKycFacialStatus("VERIFIED");
        when(claimSessionRepository.findByBeneficiaryContactIdAndTokenStatus(
                "beneficiary-uuid-1", "ACTIVE")).thenReturn(java.util.List.of(claim));
    }
}
