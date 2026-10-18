package com.legacyvault.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "legal_verification_documents")
public class LegalVerificationDocument {

    @Id
    @Column(name = "document_id", length = 36, columnDefinition = "CHAR(36)")
    private String documentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_id", nullable = false, columnDefinition = "CHAR(36)")
    private LegalVerificationCase legalVerificationCase;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 40)
    private DocumentType documentType;

    @Column(name = "storage_key", nullable = false, length = 512, unique = true)
    private String storageKey;

    @Column(name = "display_filename", nullable = false, length = 255)
    private String displayFilename;

    @Column(name = "media_type", nullable = false, length = 127)
    private String mediaType;

    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;

    @Column(name = "sha256", nullable = false, length = 64, columnDefinition = "CHAR(64)")
    private String sha256;

    @Column(name = "uploaded_by_user_id", nullable = false, length = 36, columnDefinition = "CHAR(36)")
    private String uploadedByUserId;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    protected LegalVerificationDocument() {
    }

    public LegalVerificationDocument(String documentId, LegalVerificationCase legalVerificationCase,
                                     DocumentType documentType, String storageKey, String displayFilename,
                                     String mediaType, long fileSizeBytes, String sha256, String uploadedByUserId) {
        this.documentId = documentId;
        this.legalVerificationCase = legalVerificationCase;
        this.documentType = documentType;
        this.storageKey = storageKey;
        this.displayFilename = displayFilename;
        this.mediaType = mediaType;
        this.fileSizeBytes = fileSizeBytes;
        this.sha256 = sha256;
        this.uploadedByUserId = uploadedByUserId;
    }

    @PrePersist
    void onCreate() { uploadedAt = Instant.now(); }

    public String getDocumentId() { return documentId; }
    public LegalVerificationCase getLegalVerificationCase() { return legalVerificationCase; }
    public DocumentType getDocumentType() { return documentType; }
    public String getStorageKey() { return storageKey; }
    public String getDisplayFilename() { return displayFilename; }
    public String getMediaType() { return mediaType; }
    public long getFileSizeBytes() { return fileSizeBytes; }
    public String getSha256() { return sha256; }
    public String getUploadedByUserId() { return uploadedByUserId; }
    public Instant getUploadedAt() { return uploadedAt; }
}
