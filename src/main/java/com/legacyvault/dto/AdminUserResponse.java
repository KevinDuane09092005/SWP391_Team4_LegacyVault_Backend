package com.legacyvault.dto;

import java.time.LocalDateTime;

public record AdminUserResponse(
        String id,
        String fullName,
        String email,
        String identifier,
        String role,
        String status,
        LocalDateTime createdAt) {
}
