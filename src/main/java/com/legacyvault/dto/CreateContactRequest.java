package com.legacyvault.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateContactRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name cannot exceed 100 characters")
    @JsonAlias({"full_name"})
    private String fullName;

    @NotBlank(message = "Relationship is required")
    @Size(max = 50, message = "Relationship cannot exceed 50 characters")
    private String relationship;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 150, message = "Email cannot exceed 150 characters")
    private String email;

    @Size(max = 20, message = "Phone number cannot exceed 20 characters")
    @JsonAlias({"phone_number"})
    private String phoneNumber;

    @JsonAlias({"expected_role"})
    private String expectedRole; // "Beneficiary", "Executor", "Both"

    public CreateContactRequest() {}

    public CreateContactRequest(String fullName, String relationship, String email,
                                String expectedRole) {
        this.fullName = fullName;
        this.relationship = relationship;
        this.email = email;
        this.expectedRole = expectedRole;
    }

    public CreateContactRequest(String fullName, String relationship, String email,
                                String phoneNumber, String expectedRole) {
        this.fullName = fullName;
        this.relationship = relationship;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.expectedRole = expectedRole;
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

    public String getExpectedRole() {
        return expectedRole;
    }

    public void setExpectedRole(String expectedRole) {
        this.expectedRole = expectedRole;
    }
}
