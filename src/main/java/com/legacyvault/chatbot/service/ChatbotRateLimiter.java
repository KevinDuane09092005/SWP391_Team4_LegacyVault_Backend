package com.legacyvault.chatbot.service;

import com.legacyvault.chatbot.config.ChatbotProperties;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class ChatbotRateLimiter {

    private static final long WINDOW_MILLIS = 60_000L;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final ChatbotProperties properties;

    public ChatbotRateLimiter(ChatbotProperties properties) {
        this.properties = properties;
    }

    public void checkMessageLimit(String userId) {
        check("message:" + userId, properties.getMessagesPerMinute());
    }

    public void checkSuggestionLimit(String userId) {
        check("suggestions:" + userId, properties.getSuggestionsPerMinute());
    }

    private void check(String key, int limit) {
        long now = System.currentTimeMillis();
        AtomicBoolean exceeded = new AtomicBoolean(false);
        windows.compute(key, (ignored, current) -> {
            if (current == null || now - current.startedAt() >= WINDOW_MILLIS) {
                return new Window(now, 1);
            }
            if (current.count() >= Math.max(1, limit)) {
                exceeded.set(true);
                return current;
            }
            return new Window(current.startedAt(), current.count() + 1);
        });
        if (windows.size() > 2048) {
            windows.entrySet().removeIf(entry -> now - entry.getValue().startedAt() >= WINDOW_MILLIS * 2);
        }
        if (exceeded.get()) {
            throw ChatbotException.rateLimited();
        }
    }

    private record Window(long startedAt, int count) {
    }
}
