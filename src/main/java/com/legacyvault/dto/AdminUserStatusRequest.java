package com.legacyvault.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminUserStatusRequest(@NotBlank String status) {
}

