package com.legacyvault.service;

import com.legacyvault.dto.AssetResponse;
import com.legacyvault.dto.CreateAssetRequest;
import com.legacyvault.entity.Asset;
import com.legacyvault.repository.AssetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AssetServiceTest {

    @Mock
    private AssetRepository assetRepository;

    private EncryptionService encryptionService;
    private AssetService assetService;
    private Asset testAsset;

    @BeforeEach
    void setUp() {
        encryptionService = new EncryptionService("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
        assetService = new AssetService(assetRepository, encryptionService);

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
}
