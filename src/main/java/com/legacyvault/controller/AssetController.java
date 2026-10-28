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
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
            getAuthenticatedUserId(authentication);
            if (fileStorageService == null) {
                throw new IllegalStateException("File storage service is not available");
            }
            StoredFileInfo stored = fileStorageService.storeFile(file, "asset-documents");
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
    public ResponseEntity<Resource> viewOrDownloadAssetDocument(
            @PathVariable("storedFileName") String storedFileName) {
        try {
            if (fileStorageService == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            Resource resource = fileStorageService.loadFileAsResource("asset-documents/" + storedFileName);
            int underscoreIdx = storedFileName.indexOf('_');
            String displayFileName = underscoreIdx > 0 && underscoreIdx < storedFileName.length() - 1
                    ? storedFileName.substring(underscoreIdx + 1)
                    : storedFileName;

            String lower = displayFileName.toLowerCase(Locale.ROOT);
            String contentType = "application/octet-stream";
            if (lower.endsWith(".pdf")) contentType = "application/pdf";
            else if (lower.endsWith(".png")) contentType = "image/png";
            else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) contentType = "image/jpeg";
            else if (lower.endsWith(".txt")) contentType = "text/plain; charset=UTF-8";
            else if (lower.endsWith(".docx")) contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

            ContentDisposition disposition = lower.endsWith(".docx")
                    ? ContentDisposition.attachment().filename(displayFileName, StandardCharsets.UTF_8).build()
                    : ContentDisposition.inline().filename(displayFileName, StandardCharsets.UTF_8).build();

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
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
