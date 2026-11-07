package com.legacyvault.chatbot.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ChatbotMessageRequest(
        @NotBlank
        @Size(max = 1000)
        String message,
        @Pattern(regexp = "en|vi")
        String language,
        @Size(max = 8)
        List<@Valid ChatbotHistoryTurn> history) {
}
