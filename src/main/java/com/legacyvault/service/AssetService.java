package com.legacyvault.service;

import com.legacyvault.dto.AssetResponse;
import com.legacyvault.dto.CreateAssetRequest;
import com.legacyvault.dto.UpdateAssetRequest;
import com.legacyvault.entity.Asset;
import com.legacyvault.repository.AssetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AssetService {

    private final AssetRepository assetRepository;
    private final EncryptionService encryptionService;

    public AssetService(AssetRepository assetRepository, EncryptionService encryptionService) {
        this.assetRepository = assetRepository;
        this.encryptionService = encryptionService;
    }

    @Transactional
    public AssetResponse createAsset(String ownerId, CreateAssetRequest request) {
        if (ownerId == null || ownerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Owner ID is required");
        }

        String encryptedCredentials = null;
        if (request.getCredentials() != null && !request.getCredentials().trim().isEmpty()) {
            encryptedCredentials = encryptionService.encrypt(request.getCredentials());
        }

        Asset asset = new Asset(
                ownerId,
                request.getAssetType(),
                request.getAssetName(),
                request.getDescription(),
                encryptedCredentials,
                request.getAttachmentUrl()
        );

        Asset savedAsset = assetRepository.save(asset);
        return mapToResponse(savedAsset, false);
    }

    @Transactional(readOnly = true)
    public List<AssetResponse> getAssetsByOwner(String ownerId) {
        if (ownerId == null || ownerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Owner ID is required");
        }
        return assetRepository.findByOwnerId(ownerId)
                .stream()
                .map(asset -> mapToResponse(asset, false))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AssetResponse getAssetDetail(String ownerId, String assetId) {
        Asset asset = assetRepository.findByAssetIdAndOwnerId(assetId, ownerId)
                .orElseThrow(() -> new RuntimeException("Asset not found or access denied"));

        String decryptedCredentials = null;
        if (asset.getEncryptedCredentials() != null) {
            decryptedCredentials = encryptionService.decrypt(asset.getEncryptedCredentials());
        }

        AssetResponse response = mapToResponse(asset, true);
        response.setDecryptedCredentials(decryptedCredentials);
        return response;
    }

    @Transactional
    public AssetResponse updateAsset(String ownerId, String assetId, UpdateAssetRequest request) {
        if (ownerId == null || ownerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Owner ID is required");
        }
        if (assetId == null || assetId.trim().isEmpty()) {
            throw new IllegalArgumentException("Asset ID is required");
        }
        if (request == null) {
            throw new IllegalArgumentException("Request body cannot be null");
        }

        Asset asset = assetRepository.findByAssetIdAndOwnerId(assetId, ownerId)
                .orElseThrow(() -> new RuntimeException("Asset not found or access denied"));

        if (request.getAssetName() != null && !request.getAssetName().trim().isEmpty()) {
            asset.setAssetName(request.getAssetName().trim());
        }

        if (request.getAssetType() != null && !request.getAssetType().trim().isEmpty()) {
            asset.setAssetType(request.getAssetType().trim());
        }

        asset.setDescription(request.getDescription());

        // Update encryptedCredentials only if new credentials are provided
        if (request.getCredentials() != null && !request.getCredentials().trim().isEmpty()) {
            asset.setEncryptedCredentials(encryptionService.encrypt(request.getCredentials()));
        }

        if (request.getAttachmentUrl() != null) {
            asset.setAttachmentUrl(request.getAttachmentUrl());
        }

        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            asset.setStatus(request.getStatus().trim());
        }

        asset.setUpdatedAt(LocalDateTime.now());
        Asset savedAsset = assetRepository.save(asset);

        String decryptedCredentials = null;
        if (savedAsset.getEncryptedCredentials() != null) {
            decryptedCredentials = encryptionService.decrypt(savedAsset.getEncryptedCredentials());
        }

        AssetResponse response = mapToResponse(savedAsset, true);
        response.setDecryptedCredentials(decryptedCredentials);
        return response;
    }

    private AssetResponse mapToResponse(Asset asset, boolean includeDecrypted) {
        return new AssetResponse(
                asset.getAssetId(),
                asset.getOwnerId(),
                asset.getAssetType(),
                asset.getAssetName(),
                asset.getDescription(),
                null, // Credentials not decrypted by default in list summary
                asset.getAttachmentUrl(),
                asset.getStatus(),
                asset.getCreatedAt(),
                asset.getUpdatedAt()
        );
    }
}
