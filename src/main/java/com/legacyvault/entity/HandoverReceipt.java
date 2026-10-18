package com.legacyvault.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;
import java.time.LocalDateTime;

@Entity
@Table(name = "handover_receipts")
public class HandoverReceipt {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "receipt_id", updatable = false, nullable = false, columnDefinition = "CHAR(36)")
    private String receiptId;

    @Column(name = "case_id", nullable = false, columnDefinition = "CHAR(36)")
    private String caseId;

    @Column(name = "asset_id", nullable = false, columnDefinition = "CHAR(36)")
    private String assetId;

    @Column(name = "beneficiary_contact_id", nullable = false, columnDefinition = "CHAR(36)")
    private String beneficiaryContactId;

    @Column(name = "digital_receipt_hash", nullable = false, length = 64)
    private String digitalReceiptHash;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "feedback_notes", columnDefinition = "TEXT")
    private String feedbackNotes;

    @Column(name = "confirmed_at", updatable = false)
    private LocalDateTime confirmedAt;

    public HandoverReceipt() {}

    public HandoverReceipt(String caseId, String assetId, String beneficiaryContactId, String digitalReceiptHash) {
        this.caseId = caseId;
        this.assetId = assetId;
        this.beneficiaryContactId = beneficiaryContactId;
        this.digitalReceiptHash = digitalReceiptHash;
    }

    public String getReceiptId() {
        return receiptId;
    }

    public void setReceiptId(String receiptId) {
        this.receiptId = receiptId;
    }

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public String getBeneficiaryContactId() {
        return beneficiaryContactId;
    }

    public void setBeneficiaryContactId(String beneficiaryContactId) {
        this.beneficiaryContactId = beneficiaryContactId;
    }

    public String getDigitalReceiptHash() {
        return digitalReceiptHash;
    }

    public void setDigitalReceiptHash(String digitalReceiptHash) {
        this.digitalReceiptHash = digitalReceiptHash;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getFeedbackNotes() {
        return feedbackNotes;
    }

    public void setFeedbackNotes(String feedbackNotes) {
        this.feedbackNotes = feedbackNotes;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(LocalDateTime confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    @PrePersist
    protected void onCreate() {
        confirmedAt = LocalDateTime.now();
    }
}
