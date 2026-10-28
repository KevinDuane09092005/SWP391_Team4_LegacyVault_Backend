package com.legacyvault.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record AdminUserResponse(
        String id,
        @JsonProperty("user_id") String userId,
        String fullName,
        String name,
        String identifier,
        String email,
        String role,
        String status,
        LocalDateTime createdAt) {
}

