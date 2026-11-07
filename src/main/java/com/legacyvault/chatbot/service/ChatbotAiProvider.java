package com.legacyvault.chatbot.service;

import com.legacyvault.chatbot.dto.ChatbotAiReply;
import com.legacyvault.chatbot.dto.ChatbotHistoryTurn;

import java.util.List;

public interface ChatbotAiProvider {
    ChatbotAiReply answer(String instructions, String message, List<ChatbotHistoryTurn> history);
}
