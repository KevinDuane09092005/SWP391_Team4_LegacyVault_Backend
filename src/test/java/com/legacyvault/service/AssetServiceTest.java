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
import com.legacyvault.entity.User;
import com.legacyvault.repository.AssetAssignmentRepository;
import com.legacyvault.repository.AssetRepository;
import com.legacyvault.repository.AssignmentBeneficiaryRepository;
import com.legacyvault.repository.BeneficiaryClaimSessionRepository;
import com.legacyvault.repository.ContactRepository;
import com.legacyvault.repository.HandoverCaseRepository;
import com.legacyvault.repository.HandoverReceiptRepository;
import com.legacyvault.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AssetServiceTest {

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private AssetAssignmentRepository assetAssignmentRepository;

    @Mock
    private AssignmentBeneficiaryRepository assignmentBeneficiaryRepository;

    @Mock
    private HandoverCaseRepository handoverCaseRepository;

    @Mock
    private BeneficiaryClaimSessionRepository claimSessionRepository;

    @Mock
    private HandoverReceiptRepository handoverReceiptRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ContactRepository contactRepository;

    private EncryptionService encryptionService;
    private AssetService assetService;
    private Asset testAsset;

    @BeforeEach
    void setUp() {
        encryptionService = new EncryptionService("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
        assetService = new AssetService(
                assetRepository,
                encryptionService,
                fileStorageService,
                assetAssignmentRepository,
                assignmentBeneficiaryRepository,
                handoverCaseRepository,
                claimSessionRepository,
                handoverReceiptRepository,
                userRepository,
                contactRepository
        );

        String encryptedPayload = encryptionService.encrypt("raw_password_123");
        testAsset = new Asset(
                "owner-uuid",
                "Bank Account",
                "Primary Savings",
                "Account at Chase",
                encryptedPayload,
                "https://storage.example.com/doc.pdf"
        );
        testAsset.setAssetId("asset-uuid-1");
    }

    @Test
    void testCreateAsset_Success() {
        CreateAssetRequest request = new CreateAssetRequest(
                "Bank Account",
                "Primary Savings",
                "Account at Chase",
                "raw_password_123",
                "https://storage.example.com/doc.pdf"
        );

        when(assetRepository.save(any(Asset.class))).thenAnswer(invocation -> {
            Asset a = invocation.getArgument(0);
            a.setAssetId("generated-uuid");
            return a;
        });

        AssetResponse response = assetService.createAsset("owner-uuid", request);

        assertNotNull(response);
        assertEquals("Primary Savings", response.getAssetName());
        assertEquals("Bank Account", response.getAssetType());
        verify(assetRepository, times(1)).save(any(Asset.class));
    }

    @Test
    void testGetAssetsByOwner_Success() {
        when(assetRepository.findByOwnerId("owner-uuid")).thenReturn(List.of(testAsset));

        List<AssetResponse> responses = assetService.getAssetsByOwner("owner-uuid");

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("Primary Savings", responses.get(0).getAssetName());
        assertNull(responses.get(0).getDecryptedCredentials(), "Credentials should not be decrypted in list summary");
        verify(assetRepository, times(1)).findByOwnerId("owner-uuid");
    }

    @Test
    void testGetAssetsByOwner_FilterByStatusAndType() {
        Asset assignedCrypto = new Asset("owner-uuid", "Crypto Wallet", "MetaMask", "ETH", "enc", null);
        assignedCrypto.setAssetId("asset-uuid-2");
        assignedCrypto.setStatus("assigned");

        when(assetRepository.findByOwnerId("owner-uuid")).thenReturn(List.of(testAsset, assignedCrypto));

        List<AssetResponse> responses = assetService.getAssetsByOwner("owner-uuid", "assigned", "crypto_wallet");

        assertEquals(1, responses.size());
        assertEquals("MetaMask", responses.get(0).getAssetName());
    }

    @Test
    void testGetAssetDetail_Success() {
        when(assetRepository.findByAssetIdAndOwnerId("asset-uuid-1", "owner-uuid"))
                .thenReturn(Optional.of(testAsset));

        AssetResponse response = assetService.getAssetDetail("owner-uuid", "asset-uuid-1");

        assertNotNull(response);
        assertEquals("asset-uuid-1", response.getAssetId());
        assertEquals("raw_password_123", response.getDecryptedCredentials());
    }

    @Test
    void testGetAssetDetail_NotFoundOrAccessDenied() {
        when(assetRepository.findByAssetIdAndOwnerId("asset-uuid-1", "wrong-owner-id"))
                .thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () ->
                assetService.getAssetDetail("wrong-owner-id", "asset-uuid-1")
        );

        assertTrue(exception.getMessage().contains("Asset not found or access denied"));
    }

    @Test
    void testUpdateAsset_Success_WithNewCredentials() {
        when(assetRepository.findByAssetIdAndOwnerId("asset-uuid-1", "owner-uuid"))
                .thenReturn(Optional.of(testAsset));
        when(assetRepository.save(any(Asset.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateAssetRequest request = new UpdateAssetRequest(
                "Crypto Wallet",
                "Updated Wallet Name",
                "Updated description",
                "new_secret_key_999",
                "https://storage.example.com/updated_doc.pdf"
        );

        AssetResponse response = assetService.updateAsset("owner-uuid", "asset-uuid-1", request);

        assertNotNull(response);
        assertEquals("asset-uuid-1", response.getAssetId());
        assertEquals("Updated Wallet Name", response.getAssetName());
        assertEquals("Crypto Wallet", response.getAssetType());
        assertEquals("Updated description", response.getDescription());
        assertEquals("new_secret_key_999", response.getDecryptedCredentials());
        assertEquals("https://storage.example.com/updated_doc.pdf", response.getAttachmentUrl());
        verify(assetRepository, times(1)).save(testAsset);
    }

    @Test
    void testUpdateAsset_Success_KeepExistingCredentialsWhenNull() {
        when(assetRepository.findByAssetIdAndOwnerId("asset-uuid-1", "owner-uuid"))
                .thenReturn(Optional.of(testAsset));
        when(assetRepository.save(any(Asset.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateAssetRequest request = new UpdateAssetRequest(
                "Bank Account",
                "Renamed Account",
                "New Description",
                null, // No new credentials provided
                "https://storage.example.com/doc.pdf"
        );

        AssetResponse response = assetService.updateAsset("owner-uuid", "asset-uuid-1", request);

        assertNotNull(response);
        assertEquals("Renamed Account", response.getAssetName());
        // Should keep existing decrypted password
        assertEquals("raw_password_123", response.getDecryptedCredentials());
        verify(assetRepository, times(1)).save(testAsset);
    }

    @Test
    void testUpdateAsset_NotFoundOrAccessDenied() {
        when(assetRepository.findByAssetIdAndOwnerId("asset-uuid-1", "wrong-owner-id"))
                .thenReturn(Optional.empty());

        UpdateAssetRequest request = new UpdateAssetRequest(
                "Bank Account",
                "Renamed Account",
                "New Description",
                "pass",
                null
        );

        Exception exception = assertThrows(RuntimeException.class, () ->
                assetService.updateAsset("wrong-owner-id", "asset-uuid-1", request)
        );

        assertTrue(exception.getMessage().contains("Asset not found or access denied"));
    }

    @Test
    void testDeleteAsset_Success() {
        when(assetRepository.findByAssetIdAndOwnerId("asset-uuid-1", "owner-uuid"))
                .thenReturn(Optional.of(testAsset));

        assertDoesNotThrow(() -> assetService.deleteAsset("owner-uuid", "asset-uuid-1"));

        verify(assetRepository, times(1)).delete(testAsset);
    }

    @Test
    void testDeleteAsset_NotFound_ThrowsException() {
        when(assetRepository.findByAssetIdAndOwnerId("asset-uuid-1", "wrong-owner-id"))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                assetService.deleteAsset("wrong-owner-id", "asset-uuid-1")
        );

        assertTrue(ex.getMessage().contains("Asset not found or access denied"));
        verify(assetRepository, never()).delete(any());
    }

    @Test
    void testStoreEncryptedAssetDocument_StoresAsEncAndPreservesOriginalFileName() throws Exception {
        byte[] rawPdf = "%PDF-1.7-secret-legal-will-content".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "filea.pdf", "application/pdf", rawPdf);

        ArgumentCaptor<MultipartFile> fileCaptor = ArgumentCaptor.forClass(MultipartFile.class);
        when(fileStorageService.storeFile(fileCaptor.capture(), eq("asset-documents"))).thenAnswer(inv -> {
            MultipartFile passed = inv.getArgument(0);
            return new StoredFileInfo(
                    "550e8400-e29b-41d4-a716-446655440000_" + passed.getOriginalFilename(),
                    passed.getOriginalFilename(),
                    "asset-documents/550e8400-e29b-41d4-a716-446655440000_" + passed.getOriginalFilename(),
                    passed.getSize(),
                    passed.getContentType(),
                    "sha256"
            );
        });

        StoredFileInfo result = assetService.storeEncryptedAssetDocument("owner-uuid", file);

        assertEquals("filea.pdf", result.getOriginalFileName());
        assertEquals("550e8400-e29b-41d4-a716-446655440000_filea.pdf.enc", result.getStoredFileName());
        assertEquals(rawPdf.length, result.getSizeBytes());

        MultipartFile capturedEncryptedFile = fileCaptor.getValue();
        assertEquals("filea.pdf.enc", capturedEncryptedFile.getOriginalFilename());
        byte[] encryptedPayload = capturedEncryptedFile.getBytes();
        assertFalse(java.util.Arrays.equals(rawPdf, encryptedPayload));
        assertArrayEquals(rawPdf, encryptionService.decryptBytes(encryptedPayload));
    }

    @Test
    void testUpdateAsset_DeletesPhysicallyRemovedDocumentFilesOnly() {
        String keptUrl = "http://localhost:8080/api/v1/assets/documents/1111_kept.pdf.enc";
        String removedUrl = "http://localhost:8080/api/v1/assets/documents/2222_removed.pdf.enc";
        testAsset.setAttachmentUrl(keptUrl + ", " + removedUrl);

        when(assetRepository.findByAssetIdAndOwnerId("asset-uuid-1", "owner-uuid"))
                .thenReturn(Optional.of(testAsset));
        when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateAssetRequest request = new UpdateAssetRequest(
                "Bank Account",
                "Primary Savings",
                "Account at Chase",
                null,
                keptUrl
        );

        assetService.updateAsset("owner-uuid", "asset-uuid-1", request);

        verify(fileStorageService, times(1)).deleteFile("asset-documents/2222_removed.pdf.enc");
        verify(fileStorageService, never()).deleteFile("asset-documents/1111_kept.pdf.enc");
    }

    @Test
    void testDeleteAsset_DeletesAllAttachedDocumentFiles() {
        String doc1 = "http://localhost:8080/api/v1/assets/documents/1111_doc1.pdf.enc";
        String doc2 = "http://localhost:8080/api/v1/assets/documents/2222_doc2.png.enc";
        testAsset.setAttachmentUrl(doc1 + ", " + doc2);

        when(assetRepository.findByAssetIdAndOwnerId("asset-uuid-1", "owner-uuid"))
                .thenReturn(Optional.of(testAsset));

        assetService.deleteAsset("owner-uuid", "asset-uuid-1");

        verify(assetRepository, times(1)).delete(testAsset);
        verify(fileStorageService, times(1)).deleteFile("asset-documents/1111_doc1.pdf.enc");
        verify(fileStorageService, times(1)).deleteFile("asset-documents/2222_doc2.png.enc");
    }

    @Test
    void testLoadDecryptedAssetDocument_OwnerAndDesignatedBeneficiaryAndUnauthorizedAccess() throws Exception {
        String storedFileName = "550e8400-e29b-41d4-a716-446655440000_filea.pdf.enc";
        byte[] rawContent = "Confidential Legal Estate Document".getBytes(StandardCharsets.UTF_8);
        byte[] encryptedContent = encryptionService.encryptBytes(rawContent);

        testAsset.setAttachmentUrl("http://localhost:8080/api/v1/assets/documents/" + storedFileName);
        when(assetRepository.findById("asset-uuid-1")).thenReturn(Optional.of(testAsset));
        when(fileStorageService.loadFileAsResource("asset-documents/" + storedFileName))
                .thenReturn(new ByteArrayResource(encryptedContent));

        // 1. Vault Owner can decrypt and download
        AssetService.DecryptedAssetDocument ownerDoc = assetService.loadDecryptedAssetDocument(
                storedFileName, "owner-uuid", "asset-uuid-1", null);
        assertEquals("filea.pdf", ownerDoc.getDisplayFileName());
        assertEquals("application/pdf", ownerDoc.getContentType());
        assertArrayEquals(rawContent, ownerDoc.getContent());

        // 2. Setup assignment to Beneficiary 1 (contact-b1 / user-b1) ONLY
        AssetAssignment assignment = new AssetAssignment("asset-uuid-1", "exec-contact-1", "Release after KYC");
        assignment.setAssignmentId("assign-1");
        when(assetAssignmentRepository.findByAssetId("asset-uuid-1")).thenReturn(Optional.of(assignment));
        when(assignmentBeneficiaryRepository.findByAssignmentId("assign-1"))
                .thenReturn(List.of(new AssignmentBeneficiary("assign-1", "contact-b1")));

        Contact contactB1 = new Contact("owner-uuid", "Linh Pham", "Daughter", "linh@example.com", null, true, false, "verified");
        contactB1.setContactId("contact-b1");
        Contact contactB2 = new Contact("owner-uuid", "Binh Pham", "Son", "binh@example.com", null, true, false, "verified");
        contactB2.setContactId("contact-b2");
        Contact executorContact = new Contact("owner-uuid", "Minh Le", "Executor", "exec@example.com", null, false, true, "verified");
        executorContact.setContactId("exec-contact-1");
        when(contactRepository.findByOwnerId("owner-uuid")).thenReturn(List.of(contactB1, contactB2, executorContact));

        User userB1 = new User();
        userB1.setId("user-b1");
        userB1.setEmail("linh@example.com");
        lenient().when(userRepository.findById("user-b1")).thenReturn(Optional.of(userB1));

        User userB2 = new User();
        userB2.setId("user-b2");
        userB2.setEmail("binh@example.com");
        lenient().when(userRepository.findById("user-b2")).thenReturn(Optional.of(userB2));

        User executorUser = new User();
        executorUser.setId("user-exec");
        executorUser.setEmail("exec@example.com");
        lenient().when(userRepository.findById("user-exec")).thenReturn(Optional.of(executorUser));

        HandoverCase handoverCase = new HandoverCase("LV-1042", "owner-uuid", "dms-1");
        handoverCase.setCaseId("case-1042");
        handoverCase.setStatus("RELEASED");
        lenient().when(handoverCaseRepository.findByCaseNumber("LV-1042")).thenReturn(Optional.of(handoverCase));

        BeneficiaryClaimSession sessionB1 = new BeneficiaryClaimSession();
        sessionB1.setCaseId("case-1042");
        sessionB1.setBeneficiaryContactId("contact-b1");
        sessionB1.setTokenStatus("ACTIVE");
        sessionB1.setIdentityVerified(true);
        sessionB1.setKycFacialStatus("VERIFIED");
        lenient().when(claimSessionRepository.findByCaseId("case-1042")).thenReturn(List.of(sessionB1));

        // 3. Designated Beneficiary 1 (after OTP + Face KYC + RELEASED) can decrypt and download
        AssetService.DecryptedAssetDocument b1Doc = assetService.loadDecryptedAssetDocument(
                storedFileName, "user-b1", "asset-uuid-1", "LV-1042");
        assertEquals("filea.pdf", b1Doc.getDisplayFileName());
        assertArrayEquals(rawContent, b1Doc.getContent());

        // 4. Beneficiary 2 (not assigned to this asset) is FORBIDDEN (403)
        ResponseStatusException b2Ex = assertThrows(ResponseStatusException.class, () ->
                assetService.loadDecryptedAssetDocument(storedFileName, "user-b2", "asset-uuid-1", "LV-1042"));
        assertEquals(HttpStatus.FORBIDDEN, b2Ex.getStatusCode());

        // 5. Executor is FORBIDDEN (403)
        ResponseStatusException execEx = assertThrows(ResponseStatusException.class, () ->
                assetService.loadDecryptedAssetDocument(storedFileName, "user-exec", "asset-uuid-1", "LV-1042"));
        assertEquals(HttpStatus.FORBIDDEN, execEx.getStatusCode());

        // 6. Designated Beneficiary 1 BEFORE Face KYC or BEFORE RELEASED is FORBIDDEN (403)
        sessionB1.setKycFacialStatus("PENDING_SCAN");
        ResponseStatusException unverifiedEx = assertThrows(ResponseStatusException.class, () ->
                assetService.loadDecryptedAssetDocument(storedFileName, "user-b1", "asset-uuid-1", "LV-1042"));
        assertEquals(HttpStatus.FORBIDDEN, unverifiedEx.getStatusCode());
    }
}
