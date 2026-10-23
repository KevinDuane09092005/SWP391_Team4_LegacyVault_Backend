package com.legacyvault.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;
import java.time.LocalDateTime;

@Entity
@Table(name = "contacts")
public class Contact {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "contact_id", updatable = false, nullable = false, columnDefinition = "CHAR(36)")
    private String contactId;

    @Column(name = "owner_id", nullable = false, columnDefinition = "CHAR(36)")
    private String ownerId;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "relationship", nullable = false, length = 50)
    private String relationship;

    @Column(name = "email", nullable = false, length = 150)
    private String email;

    @Transient
    private String phoneNumber;

    @Column(name = "is_beneficiary")
    private Boolean isBeneficiary;

    @Column(name = "is_executor")
    private Boolean isExecutor;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Contact() {
    }

    public Contact(String ownerId, String fullName, String relationship, String email,
            String phoneNumber, Boolean isBeneficiary, Boolean isExecutor, String status) {
        this.ownerId = ownerId;
        this.fullName = fullName;
        this.relationship = relationship;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.isBeneficiary = isBeneficiary != null ? isBeneficiary : false;
        this.isExecutor = isExecutor != null ? isExecutor : false;
        this.status = status != null ? status : "pending";
    }

    public String getContactId() {
        return contactId;
    }

    public void setContactId(String contactId) {
        this.contactId = contactId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Boolean getIsBeneficiary() {
        return isBeneficiary;
    }

    public void setIsBeneficiary(Boolean isBeneficiary) {
        this.isBeneficiary = isBeneficiary;
    }

    public Boolean getIsExecutor() {
        return isExecutor;
    }

    public void setIsExecutor(Boolean isExecutor) {
        this.isExecutor = isExecutor;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isBeneficiary == null) {
            isBeneficiary = false;
        }
        if (isExecutor == null) {
            isExecutor = false;
        }
        if (status == null) {
            status = "pending";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
