package com.legacyvault.dto;

import jakarta.validation.constraints.NotBlank;

public class BeneficiaryDto {

    @NotBlank(message = "Beneficiary contact ID is required")
    private String contactId;

    private String fullName;
    private String email;
    private String relationship;

    public BeneficiaryDto() {}

    public BeneficiaryDto(String contactId) {
        this.contactId = contactId;
    }

    public BeneficiaryDto(String contactId, String fullName, String email, String relationship) {
        this.contactId = contactId;
        this.fullName = fullName;
        this.email = email;
        this.relationship = relationship;
    }

    public String getContactId() {
        return contactId;
    }

    public void setContactId(String contactId) {
        this.contactId = contactId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }
}
