package com.legacyvault.dto;

import java.util.UUID;

public record FaceDemoSessionResponse(UUID sessionId, int expiresInSeconds) {
}
