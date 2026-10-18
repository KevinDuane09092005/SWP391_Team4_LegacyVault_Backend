package com.legacyvault.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "system_policies")
public class SystemPolicy {

    @Id
    @Column(name = "policy_key", nullable = false, length = 100)
    private String policyKey;

    @Column(name = "policy_value", nullable = false, columnDefinition = "TEXT")
    private String policyValue;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "updated_by", columnDefinition = "CHAR(36)")
    private String updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public SystemPolicy() {}

    public SystemPolicy(String policyKey, String policyValue, String description) {
        this.policyKey = policyKey;
        this.policyValue = policyValue;
        this.description = description;
    }

    public String getPolicyKey() {
        return policyKey;
    }

    public void setPolicyKey(String policyKey) {
        this.policyKey = policyKey;
    }

    public String getPolicyValue() {
        return policyValue;
    }

    public void setPolicyValue(String policyValue) {
        this.policyValue = policyValue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
