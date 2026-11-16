package com.legacyvault.service;

import com.legacyvault.dto.StoredFileInfo;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;

@Service
@ConditionalOnProperty(name = "storage.supabase.enabled", havingValue = "false", matchIfMissing = true)
public class LocalFileStorageServiceImpl implements FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(LocalFileStorageServiceImpl.class);

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
        String originalFilename = FileStorageSupport.cleanAndValidateFilename(file);

        Path targetDir = this.rootLocation;
        if (subDirectory != null && !subDirectory.trim().isEmpty()) {
            targetDir = this.rootLocation.resolve(StringUtils.cleanPath(subDirectory)).normalize();
        }

        if (!targetDir.startsWith(this.rootLocation)) {
            throw new SecurityException("Cannot store file outside configured storage root");
        }

        Files.createDirectories(targetDir);

        String storedFileName = FileStorageSupport.buildStoredFilename(originalFilename, false);
        Path destinationFile = targetDir.resolve(storedFileName).normalize();

        if (!destinationFile.startsWith(this.rootLocation)) {
            throw new SecurityException("Cannot store file outside configured storage root");
        }

        String sha256Checksum = FileStorageSupport.computeSha256(file);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
        }

        Path relativePath = this.rootLocation.relativize(destinationFile);
        String relativePathStr = relativePath.toString().replace('\\', '/');
        String contentType = FileStorageSupport.resolveContentType(file, originalFilename);

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
}
