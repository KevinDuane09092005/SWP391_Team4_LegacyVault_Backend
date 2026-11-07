package com.legacyvault.chatbot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChatbotHistoryTurn(
        @NotBlank
        @Pattern(regexp = "user|assistant")
        String role,
        @NotBlank
        @Size(max = 1500)
        String content) {
}
