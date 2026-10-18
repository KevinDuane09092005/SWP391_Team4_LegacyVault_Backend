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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "legal_verification_events")
public class LegalVerificationEvent {

    @Id
    @Column(name = "event_id", length = 36, columnDefinition = "CHAR(36)")
    private String eventId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_id", nullable = false, columnDefinition = "CHAR(36)")
    private LegalVerificationCase legalVerificationCase;

    @Column(name = "actor_user_id", nullable = false, length = 36, columnDefinition = "CHAR(36)")
    private String actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 32)
    private CaseEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 24)
    private CaseStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", length = 24)
    private CaseStatus toStatus;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Map<String, Object> metadata;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected LegalVerificationEvent() {
    }

    public LegalVerificationEvent(String eventId, LegalVerificationCase legalVerificationCase,
                                  String actorUserId, CaseEventType eventType, CaseStatus fromStatus,
                                  CaseStatus toStatus, String reason, Map<String, Object> metadata) {
        this.eventId = eventId;
        this.legalVerificationCase = legalVerificationCase;
        this.actorUserId = actorUserId;
        this.eventType = eventType;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.reason = reason;
        this.metadata = metadata;
    }

    @PrePersist
    void onCreate() { occurredAt = Instant.now(); }

    public String getEventId() { return eventId; }
    public LegalVerificationCase getLegalVerificationCase() { return legalVerificationCase; }
    public String getActorUserId() { return actorUserId; }
    public CaseEventType getEventType() { return eventType; }
    public CaseStatus getFromStatus() { return fromStatus; }
    public CaseStatus getToStatus() { return toStatus; }
    public String getReason() { return reason; }
    public Map<String, Object> getMetadata() { return metadata; }
    public Instant getOccurredAt() { return occurredAt; }
}
