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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LegalVerificationServiceTest {

    @Mock
    private HandoverCaseRepository handoverCaseRepository;

    @Mock
    private VerificationDocumentRepository verificationDocumentRepository;

    @Mock
    private VerificationReviewRepository verificationReviewRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ContactRepository contactRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private LegalVerificationService legalVerificationService;

    private HandoverCase testCase;
    private User testOwner;
    private Contact testExecutor;
    private VerificationDocument testDoc;

    @BeforeEach
    void setUp() {
        testCase = new HandoverCase("LV-1042", "owner-1", "dms-1");
        testCase.setCaseId("case-uuid-1042");
        testCase.setStatus("SUBMITTED");
        testCase.setSubmittedAt(LocalDateTime.now().minusDays(1));

        testOwner = new User();
        testOwner.setId("owner-1");
        testOwner.setFullName("Thanh Pham");
        testOwner.setEmail("thanh.pham@vault-owner.com");

        testExecutor = new Contact("owner-1", "Linh Pham", "other", "linh.pham@beneficiary.com", "0901234567", false, true, "verified");

        testDoc = new VerificationDocument();
        testDoc.setDocumentId("doc-uuid-1");
        testDoc.setCaseId("case-uuid-1042");
        testDoc.setDocumentType("DEATH_CERTIFICATE");
        testDoc.setFileName("Death_Certificate_ThanhPham.pdf");
        testDoc.setFileSizeBytes(2100000L); // ~2.0 MB
        testDoc.setFileUrl("/api/v1/legal-verification/documents/download/doc-uuid-1");
        testDoc.setFileHashSha256("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
        testDoc.setUploadedAt(LocalDateTime.now().minusDays(1));
    }

    @Test
    void testGetVerificationQueue() {
        Page<HandoverCase> page = new PageImpl<>(List.of(testCase));
        when(handoverCaseRepository.searchVerificationQueue(any(), any(), any())).thenReturn(page);
        when(userRepository.findById("owner-1")).thenReturn(Optional.of(testOwner));
        when(contactRepository.findByOwnerIdAndIsExecutorTrue("owner-1")).thenReturn(List.of(testExecutor));
        when(verificationDocumentRepository.countByCaseId("case-uuid-1042")).thenReturn(1L);

        Page<VerificationQueueItemResponse> result = legalVerificationService.getVerificationQueue("ALL", null, PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        VerificationQueueItemResponse item = result.getContent().get(0);
        assertEquals("LV-1042", item.getCaseNumber());
        assertEquals("Thanh Pham", item.getVaultOwnerName());
        assertEquals("Linh Pham", item.getExecutorName());
        assertEquals("Submitted", item.getStatusLabel());
        assertEquals(1, item.getDocumentsCount());
    }

    @Test
    void testGetCaseDetail() {
        when(handoverCaseRepository.findById("case-uuid-1042")).thenReturn(Optional.of(testCase));
        when(userRepository.findById("owner-1")).thenReturn(Optional.of(testOwner));
        when(contactRepository.findByOwnerIdAndIsExecutorTrue("owner-1")).thenReturn(List.of(testExecutor));
        when(verificationDocumentRepository.findByCaseIdOrderByUploadedAtDesc("case-uuid-1042")).thenReturn(List.of(testDoc));

        VerificationCaseDetailResponse detail = legalVerificationService.getCaseDetail("case-uuid-1042");

        assertNotNull(detail);
        assertEquals("LV-1042", detail.getCaseNumber());
        assertEquals("Thanh Pham", detail.getVaultOwnerName());
        assertEquals("Linh Pham", detail.getExecutorName());
        assertEquals(1, detail.getDocuments().size());
        assertEquals("Death_Certificate_ThanhPham.pdf", detail.getDocuments().get(0).getFileName());
        assertTrue(detail.getReviewGuideline().contains("Confirm the document is authentic"));
    }

    @Test
    void testStartReview() {
        when(handoverCaseRepository.findById("case-uuid-1042")).thenReturn(Optional.of(testCase));
        when(handoverCaseRepository.save(any(HandoverCase.class))).thenReturn(testCase);
        when(userRepository.findById("owner-1")).thenReturn(Optional.of(testOwner));
        when(contactRepository.findByOwnerIdAndIsExecutorTrue("owner-1")).thenReturn(List.of(testExecutor));
        when(verificationDocumentRepository.findByCaseIdOrderByUploadedAtDesc("case-uuid-1042")).thenReturn(List.of(testDoc));

        VerificationCaseDetailResponse response = legalVerificationService.startReview("case-uuid-1042", "verifier-uuid-1");

        assertNotNull(response);
        assertEquals("UNDER_REVIEW", testCase.getStatus());
        assertEquals("verifier-uuid-1", testCase.getAssignedVerifierId());
        verify(handoverCaseRepository, times(1)).save(testCase);
    }

    @Test
    void testApproveCase() {
        when(handoverCaseRepository.findById("case-uuid-1042")).thenReturn(Optional.of(testCase));
        when(handoverCaseRepository.save(any(HandoverCase.class))).thenReturn(testCase);
        when(verificationReviewRepository.save(any(VerificationReview.class))).thenReturn(new VerificationReview());

        ApproveCaseRequest request = new ApproveCaseRequest();
        request.setSignatureNote("Document verified successfully.");
        request.setVerifierSignaturePayload("PKI_TEST_SIGNATURE_HASH");

        ApproveCaseResponse response = legalVerificationService.approveCase("case-uuid-1042", "verifier-uuid-1", request);

        assertNotNull(response);
        assertEquals("APPROVED", response.getStatus());
        assertEquals("Case approved", response.getApprovedTitle());
        assertTrue(response.getVaultUnlockAuthorized());
        assertNotNull(response.getVaultUnlockToken());
        assertTrue(response.getVaultUnlockToken().startsWith("vlt_unlock_"));

        verify(handoverCaseRepository, times(1)).save(testCase);
        verify(verificationReviewRepository, times(1)).save(any(VerificationReview.class));
    }

    @Test
    void testRejectCase() {
        when(handoverCaseRepository.findById("case-uuid-1042")).thenReturn(Optional.of(testCase));
        when(handoverCaseRepository.save(any(HandoverCase.class))).thenReturn(testCase);
        when(verificationReviewRepository.save(any(VerificationReview.class))).thenReturn(new VerificationReview());
        when(userRepository.findById("owner-1")).thenReturn(Optional.of(testOwner));
        when(contactRepository.findByOwnerIdAndIsExecutorTrue("owner-1")).thenReturn(List.of(testExecutor));
        when(verificationDocumentRepository.findByCaseIdOrderByUploadedAtDesc("case-uuid-1042")).thenReturn(List.of(testDoc));

        RejectCaseRequest request = new RejectCaseRequest("BLURRY_UNREADABLE", "Ảnh chụp Giấy chứng tử bị mờ.");

        VerificationCaseDetailResponse response = legalVerificationService.rejectCase("case-uuid-1042", "verifier-uuid-1", request);

        assertNotNull(response);
        assertEquals("REJECTED", testCase.getStatus());
        assertEquals("Ảnh chụp Giấy chứng tử bị mờ.", testCase.getRejectionReason());

        verify(handoverCaseRepository, times(1)).save(testCase);
        verify(verificationReviewRepository, times(1)).save(any(VerificationReview.class));
    }

    @Test
    void testUploadDocument() throws IOException {
        when(handoverCaseRepository.findById("case-uuid-1042")).thenReturn(Optional.of(testCase));
        when(verificationDocumentRepository.save(any(VerificationDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "death_cert.pdf",
                "application/pdf",
                "Sample PDF content for death certificate test".getBytes()
        );

        StoredFileInfo mockStored = new StoredFileInfo(
                "uuid-123_death_cert.pdf",
                "death_cert.pdf",
                "verification-documents/LV-1042/uuid-123_death_cert.pdf",
                file.getSize(),
                "application/pdf",
                "mock-sha256-hash"
        );
        when(fileStorageService.storeFile(eq(file), eq("verification-documents/LV-1042"))).thenReturn(mockStored);

        VerificationDocumentResponse response = legalVerificationService.uploadDocument(
                "case-uuid-1042", file, "DEATH_CERTIFICATE", "GCT-1234", "UBND Phường", LocalDate.of(2026, 10, 10));

        assertNotNull(response);
        assertEquals("death_cert.pdf", response.getFileName());
        assertEquals("DEATH_CERTIFICATE", response.getDocumentType());
        assertEquals("mock-sha256-hash", response.getFileHashSha256());
        assertNotNull(response.getDocumentId());
        assertTrue(response.getFileUrl().contains(response.getDocumentId()));
        verify(verificationDocumentRepository, times(1)).save(any(VerificationDocument.class));
        verify(fileStorageService, times(1)).storeFile(eq(file), eq("verification-documents/LV-1042"));
    }

    @Test
    void testUploadDocument_EmptyFile() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);
        when(handoverCaseRepository.findById("case-uuid-1042")).thenReturn(Optional.of(testCase));

        assertThrows(IllegalArgumentException.class, () ->
                legalVerificationService.uploadDocument("case-uuid-1042", emptyFile, "DEATH_CERTIFICATE", null, null, null));
    }

    @Test
    void testGetDocumentResource_Success() throws IOException {
        when(verificationDocumentRepository.findById("doc-uuid-1")).thenReturn(Optional.of(testDoc));
        testDoc.setStoragePath("verification-documents/LV-1042/doc.pdf");
        testDoc.setFileName("doc.pdf");

        org.springframework.core.io.ByteArrayResource resource =
                new org.springframework.core.io.ByteArrayResource("test".getBytes());
        when(fileStorageService.loadFileAsResource("verification-documents/LV-1042/doc.pdf")).thenReturn(resource);

        LegalVerificationService.DocumentResourceInfo info = legalVerificationService.getDocumentResource("doc-uuid-1");
        assertNotNull(info);
        assertEquals("doc.pdf", info.getFileName());
        assertEquals("application/pdf", info.getContentType());
        assertEquals(resource, info.getResource());
    }

    @Test
    void testGetDocumentResource_NotFound() {
        when(verificationDocumentRepository.findById("non-existent")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () ->
                legalVerificationService.getDocumentResource("non-existent"));
    }

    @Test
    void testSubmitVerificationRequest_Success() {
        when(handoverCaseRepository.findById("case-uuid-1042")).thenReturn(Optional.of(testCase));
        when(verificationDocumentRepository.existsByCaseIdAndDocumentType("case-uuid-1042", "DEATH_CERTIFICATE")).thenReturn(true);
        when(handoverCaseRepository.save(any(HandoverCase.class))).thenReturn(testCase);
        when(userRepository.findById("owner-1")).thenReturn(Optional.of(testOwner));
        when(contactRepository.findByOwnerIdAndIsExecutorTrue("owner-1")).thenReturn(List.of(testExecutor));
        when(verificationDocumentRepository.findByCaseIdOrderByUploadedAtDesc("case-uuid-1042")).thenReturn(List.of(testDoc));

        VerificationCaseDetailResponse response = legalVerificationService.submitVerificationRequest("case-uuid-1042", "Submitted by Executor");

        assertNotNull(response);
        assertEquals("SUBMITTED", testCase.getStatus());
        verify(handoverCaseRepository, times(1)).save(testCase);
    }
}
