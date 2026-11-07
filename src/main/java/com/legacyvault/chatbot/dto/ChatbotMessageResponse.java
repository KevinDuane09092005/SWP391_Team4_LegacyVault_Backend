package com.legacyvault.chatbot.dto;

import java.util.List;

public record ChatbotMessageResponse(
        String reply,
        ChatbotTopic topic,
        ChatbotReplyStatus status,
        List<String> suggestions) {
}
