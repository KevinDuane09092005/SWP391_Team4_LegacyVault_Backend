package com.legacyvault.dto;

public record RoleContextDto(
        String roleCode,
        String roleScope,
        String label,
        String subtitle,
        String vaultOwnerId,
        String vaultOwnerName
) {
}

