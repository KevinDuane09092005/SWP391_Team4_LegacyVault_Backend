package com.legacyvault.chatbot.service;

import com.legacyvault.chatbot.dto.ChatbotMessageRequest;
import com.legacyvault.chatbot.dto.ChatbotRole;
import com.legacyvault.chatbot.dto.ChatbotSuggestionsRequest;
import com.legacyvault.service.AdminSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatbotServiceTest {

    @Mock
    private ChatbotAiProvider aiProvider;

    @Mock
    private ChatbotKnowledgeBase knowledgeBase;

    @Mock
    private ChatbotRateLimiter rateLimiter;

    @Mock
    private AdminSettingsService adminSettingsService;

    private ChatbotService chatbotService;

    @BeforeEach
    void setUp() {
        chatbotService = new ChatbotService(aiProvider, knowledgeBase, rateLimiter, adminSettingsService);
    }

    @Test
    void disabledAiIntegrationRejectsMessageBeforeUsingProvider() {
        when(adminSettingsService.isIntegrationEnabled("ai-assistant")).thenReturn(false);
        ChatbotMessageRequest request = new ChatbotMessageRequest("Hello", "en", List.of());

        ChatbotException error = assertThrows(ChatbotException.class,
                () -> chatbotService.ask("user-1", ChatbotRole.VAULT_OWNER, request));

        assertEquals("AI_DISABLED", error.getCode());
        verifyNoInteractions(aiProvider, rateLimiter);
    }

    @Test
    void disabledAiIntegrationAlsoRejectsSuggestions() {
        when(adminSettingsService.isIntegrationEnabled("ai-assistant")).thenReturn(false);
        ChatbotSuggestionsRequest request = new ChatbotSuggestionsRequest("", "en");

        ChatbotException error = assertThrows(ChatbotException.class,
                () -> chatbotService.suggestions("user-1", ChatbotRole.VAULT_OWNER, request));

        assertEquals("AI_DISABLED", error.getCode());
        verifyNoInteractions(knowledgeBase, rateLimiter);
    }
}
