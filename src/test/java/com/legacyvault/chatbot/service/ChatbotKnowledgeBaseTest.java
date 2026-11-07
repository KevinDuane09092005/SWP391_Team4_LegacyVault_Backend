package com.legacyvault.chatbot.service;

import com.legacyvault.chatbot.dto.ChatbotRole;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatbotKnowledgeBaseTest {

    private final ChatbotKnowledgeBase knowledgeBase = new ChatbotKnowledgeBase(
            new ClassPathResource("chatbot/product-guidance.md"));

    @Test
    void typeaheadMatchesVietnameseTextWithoutDiacriticsIssues() {
        List<String> results = knowledgeBase.suggestions(
                "bỏ lỡ lần xác nhận", "en", ChatbotRole.VAULT_OWNER, 5);

        assertTrue(results.contains("What happens after a missed check-in?"));
    }

    @Test
    void starterQuestionsRespectAuthenticatedRole() {
        List<String> ownerQuestions = knowledgeBase.suggestions("", "en", ChatbotRole.VAULT_OWNER, 30);
        List<String> beneficiaryQuestions = knowledgeBase.suggestions("", "en", ChatbotRole.BENEFICIARY, 30);

        assertTrue(ownerQuestions.contains("How do I create or organize a vault?"));
        assertFalse(ownerQuestions.contains("What happens during legal verification?"));
        assertTrue(beneficiaryQuestions.contains("How does the beneficiary handover process work?"));
        assertFalse(beneficiaryQuestions.contains("How do I change my plan?"));
    }
}
