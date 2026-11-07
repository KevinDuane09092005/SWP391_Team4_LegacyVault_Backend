package com.legacyvault.chatbot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.legacyvault.chatbot.config.ChatbotProperties;
import com.legacyvault.chatbot.dto.ChatbotAiReply;
import com.legacyvault.chatbot.dto.ChatbotHistoryTurn;
import com.legacyvault.chatbot.dto.ChatbotReplyStatus;
import com.legacyvault.chatbot.dto.ChatbotTopic;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.POST;

class OpenAiResponsesClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void sendsStatelessStructuredRequestAndParsesReply() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ChatbotProperties properties = configuredProperties();

        ObjectNode response = objectMapper.createObjectNode();
        var output = response.putArray("output");
        var message = output.addObject();
        message.put("type", "message");
        var content = message.putArray("content");
        String modelReply = objectMapper.writeValueAsString(new ChatbotAiReply(
                "A missed check-in does not release assets immediately.",
                ChatbotTopic.DEAD_MANS_SWITCH,
                ChatbotReplyStatus.ANSWERED));
        content.addObject().put("type", "output_text").put("text", modelReply);

        server.expect(requestTo("https://api.openai.com/v1/responses"))
                .andExpect(method(POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andExpect(jsonPath("$.store").value(false))
                .andExpect(jsonPath("$.model").value("gpt-6-luna"))
                .andExpect(jsonPath("$.text.format.type").value("json_schema"))
                .andExpect(jsonPath("$.text.format.strict").value(true))
                .andExpect(jsonPath("$.tools").doesNotExist())
                .andExpect(jsonPath("$.input[0].role").value("user"))
                .andExpect(jsonPath("$.input[1].role").value("assistant"))
                .andExpect(jsonPath("$.input[2].role").value("user"))
                .andRespond(withSuccess(objectMapper.writeValueAsString(response), MediaType.APPLICATION_JSON));

        OpenAiResponsesClient client = new OpenAiResponsesClient(builder.build(), properties, objectMapper);
        ChatbotAiReply result = client.answer("Only use local help.", "What happens next?", List.of(
                new ChatbotHistoryTurn("user", "I missed my check-in."),
                new ChatbotHistoryTurn("assistant", "A review may follow.")));

        assertEquals(ChatbotTopic.DEAD_MANS_SWITCH, result.topic());
        assertEquals(ChatbotReplyStatus.ANSWERED, result.status());
        assertEquals("A missed check-in does not release assets immediately.", result.reply());
        server.verify();
    }

    @Test
    void missingApiKeyFailsBeforeMakingAnyRequest() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ChatbotProperties properties = configuredProperties();
        properties.setApiKey(" ");
        OpenAiResponsesClient client = new OpenAiResponsesClient(builder.build(), properties, objectMapper);

        ChatbotException exception = assertThrows(ChatbotException.class,
                () -> client.answer("instructions", "hello", List.of()));

        assertEquals(503, exception.getStatus().value());
        assertEquals("AI_NOT_CONFIGURED", exception.getCode());
        server.verify();
    }

    private ChatbotProperties configuredProperties() {
        ChatbotProperties properties = new ChatbotProperties();
        properties.setApiKey("test-key");
        properties.setModel("gpt-6-luna");
        return properties;
    }
}
