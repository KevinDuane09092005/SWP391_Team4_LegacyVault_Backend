package com.legacyvault.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;
import java.time.LocalDateTime;

@Entity
@Table(name = "beneficiary_claim_sessions")
public class BeneficiaryClaimSession {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "session_id", updatable = false, nullable = false, columnDefinition = "CHAR(36)")
    private String sessionId;

    @Column(name = "case_id", nullable = false, columnDefinition = "CHAR(36)")
    private String caseId;

    @Column(name = "beneficiary_contact_id", nullable = false, columnDefinition = "CHAR(36)")
    private String beneficiaryContactId;

    @Column(name = "access_code_hash", length = 64)
    private String accessCodeHash;

    @Column(name = "claim_token_hash", nullable = false, unique = true, length = 64)
    private String claimTokenHash;

    @Column(name = "otp_code_hash", length = 64)
    private String otpCodeHash;

    @Column(name = "otp_expires_at")
    private LocalDateTime otpExpiresAt;

    @Column(name = "kyc_photo_id_status", length = 50)
    private String kycPhotoIdStatus = "PENDING"; // VERIFIED, PENDING_UPLOAD, REJECTED

    @Column(name = "kyc_facial_status", length = 50)
    private String kycFacialStatus = "PENDING"; // VERIFIED, PENDING_SCAN, FAILED

    @Column(name = "identity_verified")
    private Boolean identityVerified = false;

    @Column(name = "token_status", length = 50)
    private String tokenStatus = "ACTIVE"; // ACTIVE, USED, EXPIRED, REVOKED

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public BeneficiaryClaimSession() {}

    public BeneficiaryClaimSession(String caseId, String beneficiaryContactId, String accessCodeHash, String claimTokenHash) {
        this.caseId = caseId;
        this.beneficiaryContactId = beneficiaryContactId;
        this.accessCodeHash = accessCodeHash;
        this.claimTokenHash = claimTokenHash;
        this.kycPhotoIdStatus = "PENDING";
        this.kycFacialStatus = "PENDING";
        this.identityVerified = false;
        this.tokenStatus = "ACTIVE";
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public String getBeneficiaryContactId() {
        return beneficiaryContactId;
    }

    public void setBeneficiaryContactId(String beneficiaryContactId) {
        this.beneficiaryContactId = beneficiaryContactId;
    }

    public String getAccessCodeHash() {
        return accessCodeHash;
    }

    public void setAccessCodeHash(String accessCodeHash) {
        this.accessCodeHash = accessCodeHash;
    }

    public String getClaimTokenHash() {
        return claimTokenHash;
    }

    public void setClaimTokenHash(String claimTokenHash) {
        this.claimTokenHash = claimTokenHash;
    }

    public String getOtpCodeHash() {
        return otpCodeHash;
    }

    public void setOtpCodeHash(String otpCodeHash) {
        this.otpCodeHash = otpCodeHash;
    }

    public LocalDateTime getOtpExpiresAt() {
        return otpExpiresAt;
    }

    public void setOtpExpiresAt(LocalDateTime otpExpiresAt) {
        this.otpExpiresAt = otpExpiresAt;
    }

    public String getKycPhotoIdStatus() {
        return kycPhotoIdStatus;
    }

    public void setKycPhotoIdStatus(String kycPhotoIdStatus) {
        this.kycPhotoIdStatus = kycPhotoIdStatus;
    }

    public String getKycFacialStatus() {
        return kycFacialStatus;
    }

    public void setKycFacialStatus(String kycFacialStatus) {
        this.kycFacialStatus = kycFacialStatus;
    }

    public Boolean getIdentityVerified() {
        return identityVerified;
    }

    public void setIdentityVerified(Boolean identityVerified) {
        this.identityVerified = identityVerified;
    }

    public String getTokenStatus() {
        return tokenStatus;
    }

    public void setTokenStatus(String tokenStatus) {
        this.tokenStatus = tokenStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (this.tokenStatus == null) this.tokenStatus = "ACTIVE";
        if (this.kycPhotoIdStatus == null) this.kycPhotoIdStatus = "PENDING";
        if (this.kycFacialStatus == null) this.kycFacialStatus = "PENDING";
        if (this.identityVerified == null) this.identityVerified = false;
    }
}
