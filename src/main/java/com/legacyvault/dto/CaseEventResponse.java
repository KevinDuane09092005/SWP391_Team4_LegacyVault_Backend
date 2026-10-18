package com.legacyvault.dto;

import com.legacyvault.entity.CaseEventType;
import com.legacyvault.entity.CaseStatus;

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
