package com.legacyvault.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;
import java.time.LocalDateTime;

@Entity
@Table(name = "third_party_integrations")
public class ThirdPartyIntegration {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "integration_id", updatable = false, nullable = false, columnDefinition = "CHAR(36)")
    private String integrationId;

    @Column(name = "service_name", nullable = false, length = 50, unique = true)
    private String serviceName;

    @Column(name = "status", nullable = false, length = 50)
    private String status = "HEALTHY";

    @Column(name = "config_metadata", columnDefinition = "TEXT")
    private String configMetadata;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public ThirdPartyIntegration() {
    }

    public ThirdPartyIntegration(String serviceName, String status, String configMetadata) {
        this.serviceName = serviceName;
        this.status = status != null ? status : "HEALTHY";
        this.configMetadata = configMetadata;
    }

    public ThirdPartyIntegration(String integrationId, String serviceName, String status,
                                 String configMetadata, LocalDateTime lastSyncedAt, LocalDateTime updatedAt) {
        this.integrationId = integrationId;
        this.serviceName = serviceName;
        this.status = status != null ? status : "HEALTHY";
        this.configMetadata = configMetadata;
        this.lastSyncedAt = lastSyncedAt;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = "HEALTHY";
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getIntegrationId() {
        return integrationId;
    }

    public void setIntegrationId(String integrationId) {
        this.integrationId = integrationId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getConfigMetadata() {
        return configMetadata;
    }

    public void setConfigMetadata(String configMetadata) {
        this.configMetadata = configMetadata;
    }

    public LocalDateTime getLastSyncedAt() {
        return lastSyncedAt;
    }

    public void setLastSyncedAt(LocalDateTime lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

