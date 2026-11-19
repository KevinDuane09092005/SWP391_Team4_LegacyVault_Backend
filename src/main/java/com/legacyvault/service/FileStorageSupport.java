package com.legacyvault.service;

import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

final class FileStorageSupport {

    static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".pdf", ".png", ".jpg", ".jpeg", ".docx", ".txt"
    );

    private FileStorageSupport() {
    }

    static String cleanAndValidateFilename(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store empty or null file");
        }
        String rawOriginalFilename = file.getOriginalFilename();
        String originalFilename = StringUtils.cleanPath(rawOriginalFilename != null ? rawOriginalFilename : "document.pdf");
        if (originalFilename.contains("..")) {
            throw new IllegalArgumentException(
                    "Cannot store file with relative path outside current directory: " + originalFilename);
        }
        validateFileExtension(originalFilename);
        return originalFilename;
    }

    static void validateFileExtension(String filename) {
        String lower = stripEncryptedSuffix(filename.toLowerCase(Locale.ROOT));
        boolean matched = ALLOWED_EXTENSIONS.stream().anyMatch(lower::endsWith);
        if (!matched) {
            throw new IllegalArgumentException(
                    "Invalid file extension. Allowed extensions are: " + String.join(", ", ALLOWED_EXTENSIONS));
        }
    }

    static String stripEncryptedSuffix(String filename) {
        if (filename != null && filename.toLowerCase(Locale.ROOT).endsWith(".enc")) {
            return filename.substring(0, filename.length() - 4);
        }
        return filename;
    }

    static String buildStoredFilename(String originalFilename, boolean sanitizeForUrl) {
        String safeName = sanitizeForUrl
                ? originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_")
                : originalFilename;
        return UUID.randomUUID() + "_" + safeName;
    }

    static String computeSha256(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            return bytesToHex(digest.digest());
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            return "UNKNOWN_HASH";
        }
    }

    static String resolveContentType(MultipartFile file, String originalFilename) {
        String contentType = file.getContentType();
        return (contentType == null || contentType.isBlank())
                ? inferContentType(originalFilename)
                : contentType;
    }

    static String inferContentType(String filename) {
        String lower = stripEncryptedSuffix(filename.toLowerCase(Locale.ROOT));
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

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
