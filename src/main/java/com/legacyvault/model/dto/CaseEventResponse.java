package com.legacyvault.model.dto;

import com.legacyvault.model.entity.CaseEventType;
import com.legacyvault.model.entity.CaseStatus;

import java.time.Instant;

public record CaseEventResponse(
        String eventId,
        CaseEventType eventType,
        String actorUserId,
        CaseStatus fromStatus,
        CaseStatus toStatus,
        String reason,
        Instant occurredAt
) {
}
