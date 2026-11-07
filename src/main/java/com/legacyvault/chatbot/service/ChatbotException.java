package com.legacyvault.chatbot.service;

import org.springframework.http.HttpStatus;

public class ChatbotException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public ChatbotException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static ChatbotException notConfigured() {
        return new ChatbotException("AI_NOT_CONFIGURED",
                "The chatbot is not configured yet. Set OPENAI_API_KEY on the backend.",
                HttpStatus.SERVICE_UNAVAILABLE);
    }

    public static ChatbotException rateLimited() {
        return new ChatbotException("RATE_LIMITED",
                "Too many chatbot requests. Please try again shortly.",
                HttpStatus.TOO_MANY_REQUESTS);
    }

    public static ChatbotException providerRateLimited() {
        return new ChatbotException("PROVIDER_RATE_LIMITED",
                "The AI provider is busy. Please try again shortly.",
                HttpStatus.TOO_MANY_REQUESTS);
    }

    public static ChatbotException providerUnavailable() {
        return new ChatbotException("PROVIDER_ERROR",
                "The AI provider is temporarily unavailable. Please try again later.",
                HttpStatus.BAD_GATEWAY);
    }
}
