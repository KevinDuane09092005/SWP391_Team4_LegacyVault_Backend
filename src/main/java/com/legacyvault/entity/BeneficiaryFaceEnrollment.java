package com.legacyvault.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "beneficiary_face_enrollments",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_face_enrollment_beneficiary",
                columnNames = "beneficiary_contact_id"))
public class BeneficiaryFaceEnrollment {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "enrollment_id", updatable = false, nullable = false, columnDefinition = "CHAR(36)")
    private String enrollmentId;

    @Column(name = "beneficiary_contact_id", nullable = false,
            columnDefinition = "CHAR(36)")
    private String beneficiaryContactId;

    @Lob
    @Column(name = "encrypted_embedding", nullable = false)
    private byte[] encryptedEmbedding;

    @Column(name = "embedding_version", nullable = false, length = 40)
    private String embeddingVersion;

    @Column(name = "consented_at", nullable = false)
    private LocalDateTime consentedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public BeneficiaryFaceEnrollment() {
    }

    public BeneficiaryFaceEnrollment(String beneficiaryContactId, byte[] encryptedEmbedding,
                                     String embeddingVersion, LocalDateTime consentedAt) {
        this.beneficiaryContactId = beneficiaryContactId;
        this.encryptedEmbedding = encryptedEmbedding;
        this.embeddingVersion = embeddingVersion;
        this.consentedAt = consentedAt;
    }

    public String getEnrollmentId() {
        return enrollmentId;
    }

    public String getBeneficiaryContactId() {
        return beneficiaryContactId;
    }

    public byte[] getEncryptedEmbedding() {
        return encryptedEmbedding;
    }

    public void setEncryptedEmbedding(byte[] encryptedEmbedding) {
        this.encryptedEmbedding = encryptedEmbedding;
    }

    public String getEmbeddingVersion() {
        return embeddingVersion;
    }

    public void setEmbeddingVersion(String embeddingVersion) {
        this.embeddingVersion = embeddingVersion;
    }

    public LocalDateTime getConsentedAt() {
        return consentedAt;
    }

    public void setConsentedAt(LocalDateTime consentedAt) {
        this.consentedAt = consentedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (consentedAt == null) {
            consentedAt = createdAt;
        }
    }
}
