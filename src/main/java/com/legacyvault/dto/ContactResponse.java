package com.legacyvault.dto;

import java.time.LocalDateTime;

public class ContactResponse {

    private String contactId;
    private String ownerId;
    private String fullName;
    private String relationship;
    private String email;
    private String phoneNumber;
    private Boolean isBeneficiary;
    private Boolean isExecutor;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ContactResponse() {}

    public ContactResponse(String contactId, String ownerId, String fullName, String relationship, 
                           String email, String phoneNumber, Boolean isBeneficiary, Boolean isExecutor, 
                           String status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.contactId = contactId;
        this.ownerId = ownerId;
        this.fullName = fullName;
        this.relationship = relationship;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.isBeneficiary = isBeneficiary;
        this.isExecutor = isExecutor;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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
}
