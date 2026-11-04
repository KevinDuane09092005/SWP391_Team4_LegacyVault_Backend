package com.legacyvault.service;

import com.legacyvault.dto.StoredFileInfo;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class LocalFileStorageServiceImpl implements FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(LocalFileStorageServiceImpl.class);

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".pdf", ".png", ".jpg", ".jpeg", ".docx", ".txt"
    );

    @Value("${storage.local.base-dir:uploads}")
    private String baseDir;

    private Path rootLocation;

    @PostConstruct
    public void init() {
        try {
            this.rootLocation = Paths.get(baseDir).toAbsolutePath().normalize();
            Files.createDirectories(this.rootLocation);
            logger.info("Local file storage initialized at: {}", this.rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage directory: " + baseDir, e);
        }
    }

    @Override
    public StoredFileInfo storeFile(MultipartFile file, String subDirectory) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store empty or null file");
        }

        String rawOriginalFilename = file.getOriginalFilename();
        String originalFilename = StringUtils.cleanPath(rawOriginalFilename != null ? rawOriginalFilename : "document.pdf");

        if (originalFilename.contains("..")) {
            throw new IllegalArgumentException("Cannot store file with relative path outside current directory: " + originalFilename);
        }

        validateFileExtension(originalFilename);

        Path targetDir = this.rootLocation;
        if (subDirectory != null && !subDirectory.trim().isEmpty()) {
            targetDir = this.rootLocation.resolve(StringUtils.cleanPath(subDirectory)).normalize();
        }

        if (!targetDir.startsWith(this.rootLocation)) {
            throw new SecurityException("Cannot store file outside configured storage root");
        }

        Files.createDirectories(targetDir);

        String storedFileName = UUID.randomUUID() + "_" + originalFilename;
        Path destinationFile = targetDir.resolve(storedFileName).normalize();

        if (!destinationFile.startsWith(this.rootLocation)) {
            throw new SecurityException("Cannot store file outside configured storage root");
        }

        String sha256Checksum;
        try (InputStream inputStream = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            sha256Checksum = bytesToHex(digest.digest());
        } catch (Exception e) {
            sha256Checksum = "UNKNOWN_HASH";
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
        }

        Path relativePath = this.rootLocation.relativize(destinationFile);
        String relativePathStr = relativePath.toString().replace('\\', '/');

        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = inferContentType(originalFilename);
        }

        logger.info("Stored file successfully: original='{}', stored='{}', path='{}', size={} bytes",
                originalFilename, storedFileName, relativePathStr, file.getSize());

        return new StoredFileInfo(
                storedFileName,
                originalFilename,
                relativePathStr,
                file.getSize(),
                contentType,
                sha256Checksum
        );
    }

    @Override
    public Resource loadFileAsResource(String relativePath) throws IOException {
        if (relativePath == null || relativePath.isBlank()) {
            throw new FileNotFoundException("File path cannot be empty");
        }

        Path filePath = this.rootLocation.resolve(StringUtils.cleanPath(relativePath)).normalize();

        if (!filePath.startsWith(this.rootLocation)) {
            throw new SecurityException("Cannot access file outside configured storage root: " + relativePath);
        }

        Resource resource = new UrlResource(filePath.toUri());
        if (resource.exists() && resource.isReadable()) {
            return resource;
        } else {
            throw new FileNotFoundException("File not found or unreadable: " + relativePath);
        }
    }

    @Override
    public void deleteFile(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        try {
            Path filePath = this.rootLocation.resolve(StringUtils.cleanPath(relativePath)).normalize();
            if (filePath.startsWith(this.rootLocation)) {
                Files.deleteIfExists(filePath);
                logger.info("Deleted file: {}", relativePath);
            }
        } catch (IOException e) {
            logger.warn("Failed to delete file {}: {}", relativePath, e.getMessage());
        }
    }

    @Override
    public boolean exists(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return false;
        }
        Path filePath = this.rootLocation.resolve(StringUtils.cleanPath(relativePath)).normalize();
        return filePath.startsWith(this.rootLocation) && Files.exists(filePath);
    }

    private void validateFileExtension(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        boolean matched = ALLOWED_EXTENSIONS.stream().anyMatch(lower::endsWith);
        if (!matched) {
            throw new IllegalArgumentException(
                    "Invalid file extension. Allowed extensions are: " + String.join(", ", ALLOWED_EXTENSIONS));
        }
    }

    private String inferContentType(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf")) {
            return "application/pdf";
        } else if (lower.endsWith(".png")) {
            return "image/png";
        } else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lower.endsWith(".txt")) {
            return "text/plain; charset=UTF-8";
        } else if (lower.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }
        return "application/octet-stream";
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
