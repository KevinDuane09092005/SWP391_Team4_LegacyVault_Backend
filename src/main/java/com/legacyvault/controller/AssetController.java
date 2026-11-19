package com.legacyvault.controller;

import com.legacyvault.dto.AssetResponse;
import com.legacyvault.dto.CreateAssetRequest;
import com.legacyvault.dto.StoredFileInfo;
import com.legacyvault.dto.UpdateAssetRequest;
import com.legacyvault.service.AssetService;
import com.legacyvault.service.FileStorageService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/assets")
public class AssetController {

    private final AssetService assetService;
    private final FileStorageService fileStorageService;

    @Value("${app.backend-url:http://localhost:8080}")
    private String backendBaseUrl;

    public AssetController(AssetService assetService) {
        this(assetService, null);
    }

    @Autowired
    public AssetController(AssetService assetService, FileStorageService fileStorageService) {
        this.assetService = assetService;
        this.fileStorageService = fileStorageService;
    }

    @PostMapping("/documents/upload")
    public ResponseEntity<?> uploadAssetDocument(
            Authentication authentication,
            @RequestParam("file") MultipartFile file) {
        try {
            String ownerId = getAuthenticatedUserId(authentication);
            StoredFileInfo stored = assetService.storeEncryptedAssetDocument(ownerId, file, fileStorageService);
            String baseUrl = (backendBaseUrl != null && !backendBaseUrl.isBlank())
                    ? backendBaseUrl.trim().replaceAll("/$", "")
                    : "http://localhost:8080";
            String fileUrl = baseUrl + "/api/v1/assets/documents/" + stored.getStoredFileName();
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "fileName", stored.getOriginalFileName(),
                    "storedFileName", stored.getStoredFileName(),
                    "fileSize", stored.getSizeBytes(),
                    "contentType", stored.getContentType(),
                    "url", fileUrl
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/documents/{storedFileName}")
    public ResponseEntity<?> viewOrDownloadAssetDocument(
            Authentication authentication,
            @PathVariable("storedFileName") String storedFileName,
            @RequestParam(value = "asset_id", required = false) String assetId,
            @RequestParam(value = "case_ref", required = false) String caseRef,
            @RequestParam(value = "download", required = false) Boolean download) {
        try {
            String requesterId = (authentication != null && authentication.getName() != null)
                    ? authentication.getName()
                    : null;
            AssetService.DecryptedAssetDocument doc = assetService.loadDecryptedAssetDocument(
                    storedFileName, requesterId, assetId, caseRef, fileStorageService);

            String displayFileName = doc.getDisplayFileName();
            String lower = displayFileName.toLowerCase(Locale.ROOT);
            boolean forceAttachment = Boolean.TRUE.equals(download) || lower.endsWith(".docx");

            ContentDisposition disposition = forceAttachment
                    ? ContentDisposition.attachment().filename(displayFileName, StandardCharsets.UTF_8).build()
                    : ContentDisposition.inline().filename(displayFileName, StandardCharsets.UTF_8).build();

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(doc.getContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                    .body(new ByteArrayResource(doc.getContent()));
        } catch (ResponseStatusException e) {
            String reason = e.getReason() != null ? e.getReason() : "Access denied";
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", reason));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Document not found"));
        }
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
    public ResponseEntity<?> getAssets(
            Authentication authentication,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "assetType", required = false) String assetType,
            @RequestParam(value = "asset_type", required = false) String assetTypeSnake) {
        try {
            String ownerId = getAuthenticatedUserId(authentication);
            String resolvedType = (assetType != null && !assetType.isBlank()) ? assetType : assetTypeSnake;
            List<AssetResponse> assets = (status == null && resolvedType == null)
                    ? assetService.getAssetsByOwner(ownerId)
                    : assetService.getAssetsByOwner(ownerId, status, resolvedType);
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

    @DeleteMapping("/{assetId}")
    public ResponseEntity<?> deleteAsset(
            Authentication authentication,
            @PathVariable("assetId") String assetId) {
        try {
            String ownerId = getAuthenticatedUserId(authentication);
            assetService.deleteAsset(ownerId, assetId);
            return ResponseEntity.ok(Map.of("message", "Asset deleted successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    private String getAuthenticatedUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("Unauthorized: No user session found");
        }
        return authentication.getName();
    }
}
