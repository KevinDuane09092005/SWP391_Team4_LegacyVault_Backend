package com.legacyvault.dto;

public class StoredFileInfo {
    private String storedFileName;
    private String originalFileName;
    private String relativePath;
    private long sizeBytes;
    private String contentType;
    private String sha256Checksum;

    public StoredFileInfo() {
    }

    public StoredFileInfo(String storedFileName, String originalFileName, String relativePath, long sizeBytes, String contentType, String sha256Checksum) {
        this.storedFileName = storedFileName;
        this.originalFileName = originalFileName;
        this.relativePath = relativePath;
        this.sizeBytes = sizeBytes;
        this.contentType = contentType;
        this.sha256Checksum = sha256Checksum;
    }

    public String getStoredFileName() {
        return storedFileName;
    }

    public void setStoredFileName(String storedFileName) {
        this.storedFileName = storedFileName;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getSha256Checksum() {
        return sha256Checksum;
    }

    public void setSha256Checksum(String sha256Checksum) {
        this.sha256Checksum = sha256Checksum;
    }
}
