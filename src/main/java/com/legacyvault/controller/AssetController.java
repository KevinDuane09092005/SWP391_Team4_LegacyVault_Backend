package com.legacyvault.controller;

import com.legacyvault.dto.AssetResponse;
import com.legacyvault.dto.CreateAssetRequest;
import com.legacyvault.dto.UpdateAssetRequest;
import com.legacyvault.service.AssetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/assets")
public class AssetController {

    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    @PostMapping
    public ResponseEntity<?> createAsset(
            Authentication authentication,
            @Valid @RequestBody CreateAssetRequest request) {
        try {
            String ownerId = getAuthenticatedUserId(authentication);
            AssetResponse created = assetService.createAsset(ownerId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "Asset created and encrypted successfully",
                    "asset", created));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getAssets(Authentication authentication) {
        try {
            String ownerId = getAuthenticatedUserId(authentication);
            List<AssetResponse> assets = assetService.getAssetsByOwner(ownerId);
            return ResponseEntity.ok(assets);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{assetId}")
    public ResponseEntity<?> getAssetDetail(
            Authentication authentication,
            @PathVariable("assetId") String assetId) {
        try {
            String ownerId = getAuthenticatedUserId(authentication);
            AssetResponse asset = assetService.getAssetDetail(ownerId, assetId);
            return ResponseEntity.ok(asset);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{assetId}")
    public ResponseEntity<?> updateAsset(
            Authentication authentication,
            @PathVariable("assetId") String assetId,
            @Valid @RequestBody UpdateAssetRequest request) {
        try {
            String ownerId = getAuthenticatedUserId(authentication);
            AssetResponse updated = assetService.updateAsset(ownerId, assetId, request);
            return ResponseEntity.ok(Map.of(
                    "message", "Asset updated successfully",
                    "asset", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private String getAuthenticatedUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("Unauthorized: No user session found");
        }
        return authentication.getName();
    }
}
