package com.legacyvault.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateAdminUserRoleRequest(
        @NotBlank
        @Pattern(regexp = "(?i)ADMIN|VAULT_OWNER|EXECUTOR|BENEFICIARY|LEGAL_VERIFIER")
        String role) {
}
