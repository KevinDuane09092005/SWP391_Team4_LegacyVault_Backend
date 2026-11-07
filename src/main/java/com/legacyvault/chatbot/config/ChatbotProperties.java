package com.legacyvault.chatbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "legacyvault.chatbot.openai")
public class ChatbotProperties {

    private String baseUrl = "https://api.openai.com/v1";
    private String apiKey = "";
    private String model = "gpt-6-luna";
    private Duration timeout = Duration.ofSeconds(20);
    private int maxOutputTokens = 700;
    private int messagesPerMinute = 20;
    private int suggestionsPerMinute = 360;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public int getMaxOutputTokens() {
        return maxOutputTokens;
    }

    public void setMaxOutputTokens(int maxOutputTokens) {
        this.maxOutputTokens = maxOutputTokens;
    }

    public int getMessagesPerMinute() {
        return messagesPerMinute;
    }

    public void setMessagesPerMinute(int messagesPerMinute) {
        this.messagesPerMinute = messagesPerMinute;
    }

    public int getSuggestionsPerMinute() {
        return suggestionsPerMinute;
    }

    public void setSuggestionsPerMinute(int suggestionsPerMinute) {
        this.suggestionsPerMinute = suggestionsPerMinute;
    }
}
