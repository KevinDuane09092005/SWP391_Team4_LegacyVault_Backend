package com.legacyvault.chatbot.dto;

public enum ChatbotRole {
    VAULT_OWNER,
    EXECUTOR,
    BENEFICIARY,
    LEGAL_VERIFIER,
    ADMIN,
    UNKNOWN;

    public static ChatbotRole fromAuthority(String authority) {
        if (authority == null) {
            return UNKNOWN;
        }
        String normalized = authority.trim().toUpperCase();
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring("ROLE_".length());
        }
        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return UNKNOWN;
        }
    }
}
