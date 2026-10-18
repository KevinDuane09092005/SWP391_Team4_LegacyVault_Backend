package com.legacyvault.dto;

import java.time.LocalDateTime;

public class VerificationDocumentResponse {
    private String documentId;
    private String documentType;
    private String documentNumber;
    private String issuedBy;
    private String issuedDate;
    private String fileName;
    private Long fileSizeBytes;
    private String fileSizeFormatted;
    private String uploadedBy;
    private String fileUrl;
    private String fileHashSha256;
    private LocalDateTime uploadedAt;

    public VerificationDocumentResponse() {
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public String getIssuedBy() {
        return issuedBy;
    }

    public void setIssuedBy(String issuedBy) {
        this.issuedBy = issuedBy;
    }

    public String getIssuedDate() {
        return issuedDate;
    }

    public void setIssuedDate(String issuedDate) {
        this.issuedDate = issuedDate;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(Long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public String getFileSizeFormatted() {
        return fileSizeFormatted;
    }

    public void setFileSizeFormatted(String fileSizeFormatted) {
        this.fileSizeFormatted = fileSizeFormatted;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getFileHashSha256() {
        return fileHashSha256;
    }

    public void setFileHashSha256(String fileHashSha256) {
        this.fileHashSha256 = fileHashSha256;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}
