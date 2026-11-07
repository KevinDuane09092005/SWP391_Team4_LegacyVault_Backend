package com.legacyvault.chatbot.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChatbotSuggestionsRequest(
        @NotNull
        @Size(max = 200)
        String query,
        @Pattern(regexp = "en|vi")
        String language) {
}
