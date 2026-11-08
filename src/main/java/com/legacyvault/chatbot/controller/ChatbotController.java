package com.legacyvault.chatbot.controller;

import com.legacyvault.chatbot.dto.ChatbotMessageRequest;
import com.legacyvault.chatbot.dto.ChatbotMessageResponse;
import com.legacyvault.chatbot.dto.ChatbotRole;
import com.legacyvault.chatbot.dto.ChatbotSuggestionsRequest;
import com.legacyvault.chatbot.dto.ChatbotSuggestionsResponse;
import com.legacyvault.chatbot.service.ChatbotService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chatbot")
public class ChatbotController {

    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping("/suggestions")
    public ChatbotSuggestionsResponse suggestions(@Valid @RequestBody ChatbotSuggestionsRequest request,
                                                  Authentication authentication) {
        return chatbotService.suggestions(authentication.getName(), role(authentication), request);
    }

    @PostMapping("/message")
    public ChatbotMessageResponse ask(@Valid @RequestBody ChatbotMessageRequest request,
                                     Authentication authentication) {
        return chatbotService.ask(authentication.getName(), role(authentication), request);
    }

    private ChatbotRole role(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(ChatbotRole::fromAuthority)
                .filter(role -> role != ChatbotRole.UNKNOWN)
                .findFirst()
                .orElse(ChatbotRole.UNKNOWN);
    }
}
