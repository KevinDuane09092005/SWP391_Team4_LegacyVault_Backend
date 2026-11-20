package com.legacyvault.service;

import com.legacyvault.dto.StoredFileInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Production FileStorageService backed by Supabase Storage REST API.
 * Supports both legacy service_role JWT keys (eyJ...) and new secret keys (sb_secret_...).
 */
@Service
@ConditionalOnProperty(name = "storage.supabase.enabled", havingValue = "true")
public class SupabaseFileStorageServiceImpl implements FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(SupabaseFileStorageServiceImpl.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final String supabaseUrl;
    private final String serviceKey;
    private final String bucket;
    private final HttpClient httpClient;

    public SupabaseFileStorageServiceImpl(
            @Value("${storage.supabase.url}") String supabaseUrl,
            @Value("${storage.supabase.service-role-key}") String serviceKey,
            @Value("${storage.supabase.bucket:legacyvault-files}") String bucket) {
        if (supabaseUrl == null || supabaseUrl.isBlank()) {
            throw new IllegalStateException("storage.supabase.url (SUPABASE_URL) must be configured when Supabase storage is enabled");
        }
        if (serviceKey == null || serviceKey.isBlank()) {
            throw new IllegalStateException("storage.supabase.service-role-key (SUPABASE_SERVICE_ROLE_KEY) must be configured when Supabase storage is enabled");
        }
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("storage.supabase.bucket (SUPABASE_STORAGE_BUCKET) must not be blank");
        }
        this.supabaseUrl = supabaseUrl.trim().replaceAll("/+$", "");
        this.serviceKey = serviceKey.trim();
        this.bucket = bucket.trim();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        logger.info("Supabase Storage initialized: url='{}', bucket='{}'", this.supabaseUrl, this.bucket);
    }

    @Override
    public StoredFileInfo storeFile(MultipartFile file, String subDirectory) throws IOException {
        String originalFilename = FileStorageSupport.cleanAndValidateFilename(file);
        String storedFileName = FileStorageSupport.buildStoredFilename(originalFilename, true);
        String cleanSubDir = normalizeObjectPath(subDirectory);
        String objectPath = cleanSubDir.isEmpty() ? storedFileName : cleanSubDir + "/" + storedFileName;

        String sha256Checksum = FileStorageSupport.computeSha256(file);
        String contentType = FileStorageSupport.resolveContentType(file, originalFilename);
        byte[] bytes = file.getBytes();

        HttpRequest request = authorizedRequest(objectUri(objectPath))
                .header("Content-Type", contentType)
                .header("x-upsert", "true")
                .POST(HttpRequest.BodyPublishers.ofByteArray(bytes))
                .build();

        HttpResponse<String> response = send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Supabase Storage upload failed (" + response.statusCode() + "): " + response.body());
        }

        logger.info("Uploaded file to Supabase Storage: bucket='{}', path='{}', size={} bytes",
                bucket, objectPath, file.getSize());

        return new StoredFileInfo(
                storedFileName,
                originalFilename,
                objectPath,
                file.getSize(),
                contentType,
                sha256Checksum
        );
    }

    @Override
    public Resource loadFileAsResource(String relativePath) throws IOException {
        String objectPath = normalizeObjectPath(relativePath);
        if (objectPath.isEmpty()) {
            throw new FileNotFoundException("File path cannot be empty");
        }

        HttpRequest request = authorizedRequest(objectUri(objectPath))
                .GET()
                .build();

        HttpResponse<byte[]> response = send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() == 400 || response.statusCode() == 404) {
            throw new FileNotFoundException("File not found or unreadable: " + relativePath);
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Supabase Storage download failed (" + response.statusCode() + ") for: " + objectPath);
        }

        return new ByteArrayResource(response.body()) {
            @Override
            public String getFilename() {
                int idx = objectPath.lastIndexOf('/');
                return idx >= 0 ? objectPath.substring(idx + 1) : objectPath;
            }
        };
    }

    @Override
    public void deleteFile(String relativePath) {
        String objectPath = normalizeObjectPath(relativePath);
        if (objectPath.isEmpty()) {
            return;
        }
        try {
            HttpRequest request = authorizedRequest(objectUri(objectPath))
                    .DELETE()
                    .build();
            HttpResponse<String> response = send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                logger.info("Deleted file from Supabase Storage: {}", objectPath);
            } else {
                logger.warn("Supabase Storage delete returned {} for {}: {}",
                        response.statusCode(), objectPath, response.body());
            }
        } catch (Exception e) {
            logger.warn("Failed to delete file {} from Supabase Storage: {}", objectPath, e.getMessage());
        }
    }

    @Override
    public boolean exists(String relativePath) {
        String objectPath = normalizeObjectPath(relativePath);
        if (objectPath.isEmpty()) {
            return false;
        }
        try {
            HttpRequest request = authorizedRequest(objectUri(objectPath))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<Void> response = send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception e) {
            logger.warn("Failed to check file existence {} in Supabase Storage: {}", objectPath, e.getMessage());
            return false;
        }
    }

    @Override
    public void overwriteFile(String relativePath, byte[] content, String contentType) throws IOException {
        String objectPath = normalizeObjectPath(relativePath);
        if (objectPath.isEmpty()) {
            throw new FileNotFoundException("File path cannot be empty");
        }
        String effectiveContentType = (contentType != null && !contentType.isBlank())
                ? contentType
                : "application/octet-stream";
        byte[] payload = content != null ? content : new byte[0];

        HttpRequest request = authorizedRequest(objectUri(objectPath))
                .header("Content-Type", effectiveContentType)
                .header("x-upsert", "true")
                .POST(HttpRequest.BodyPublishers.ofByteArray(payload))
                .build();

        HttpResponse<String> response = send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Supabase Storage overwrite failed (" + response.statusCode() + "): " + response.body());
        }
    }

    private HttpRequest.Builder authorizedRequest(URI uri) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .header("apikey", serviceKey);
        if (!serviceKey.startsWith("sb_secret_")) {
            builder.header("Authorization", "Bearer " + serviceKey);
        }
        return builder;
    }

    private URI objectUri(String objectPath) {
        String encodedBucket = encodeSegment(bucket);
        String encodedPath = Arrays.stream(objectPath.split("/"))
                .filter(segment -> !segment.isEmpty())
                .map(SupabaseFileStorageServiceImpl::encodeSegment)
                .collect(Collectors.joining("/"));
        return URI.create(supabaseUrl + "/storage/v1/object/" + encodedBucket + "/" + encodedPath);
    }

    private static String encodeSegment(String segment) {
        return URLEncoder.encode(segment, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String normalizeObjectPath(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return "";
        }
        String normalized = rawPath.trim().replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        for (String segment : normalized.split("/")) {
            if ("..".equals(segment)) {
                throw new SecurityException("Cannot access path containing '..': " + rawPath);
            }
        }
        return normalized;
    }

    private <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> handler) throws IOException {
        try {
            return httpClient.send(request, handler);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while communicating with Supabase Storage", e);
        }
    }
}
