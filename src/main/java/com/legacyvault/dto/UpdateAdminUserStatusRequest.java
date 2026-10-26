package com.legacyvault.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateAdminUserStatusRequest(
        @NotBlank
        @Pattern(regexp = "(?i)ACTIVE|SUSPENDED")
        String status) {
}
